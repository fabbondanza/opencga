#!/bin/bash
#
# Please be aware this script uploads artifacts to public blob storage with no SAS token. 
# 
# If the script is modifed to use a SAS token, be aware if the SAS token later changes then operations that depend on the storage and solution redeployment will fail.
# Given dependencies on the storage within OpenCGA a SAS token with a long lifetime needs to be created and used each time the solution is deployed.

set -e

if [[ "$#" -ne 2 && "$#" -ne 3 ]]; then
  echo "Usage: $0 <subscription_name> <main-azuredeploy-parameters-json> [<service-principal-azuredeploy-parameters-json>]"
  echo " * Execute createsp.sh to obtain the service principal parameters"
  exit 1
fi

subscriptionName=$1
azudeDeployParameters=${2:-azuredeploy.parameters.private.json}
spAzudeDeployParameters=${3:-$azudeDeployParameters}

function requiredFile() {
  if [ ! -f $1 ]; then
    echo "Missing file $1"
    exit 1
  fi
}

requiredFile "${azudeDeployParameters}"
requiredFile "${spAzudeDeployParameters}"

azudeDeployParameters=$(realpath "${azudeDeployParameters}")
spAzudeDeployParameters=$(realpath "${spAzudeDeployParameters}")

# Don't move the PWD until we found out the realpath. It could be a relative path.
cd "$(dirname "$0")"

templateContainer="templates"
location=$(jq -r '.parameters.rgLocation.value' "${azudeDeployParameters}")
rgName=$(jq -r '.parameters.rgPrefix.value' "${azudeDeployParameters}")
storageAccountName=$(echo "${rgName}artifacts" | tr '[:upper:]' '[:lower:]' | tr -d "_-")
deploymentOut="$(dirname "${azudeDeployParameters}")/deployment-outputs-$(date "+%Y%m%d%H%M%S").json"
deployId=${rgName}-$(date "+%Y-%m-%d-%H.%M.%S")-R${RANDOM}


az account set --subscription "${subscriptionName}"
az group create --name "${rgName}" --location "${location}"

echo "# Uploading file templates"

az storage account create \
    --resource-group "${rgName}" \
    --location "${location}" \
    --sku Standard_LRS \
    --kind StorageV2 \
    --name $storageAccountName

connection=$(az storage account show-connection-string \
    --resource-group "${rgName}" \
    --name $storageAccountName \
    --query connectionString)

az storage container create \
    --name $templateContainer \
    --connection-string $connection \
    --public-access blob

mkdir -p ARTIFACTS_BLOB_UPDATE/foo
rm -rf ARTIFACTS_BLOB_UPDATE/*
cp -r $(ls | grep -v "ARTIFACTS_BLOB_UPDATE\|parameters\|deployment-outputs") ARTIFACTS_BLOB_UPDATE

az storage blob upload-batch \
    --destination $templateContainer \
    --connection-string $connection \
    --source ARTIFACTS_BLOB_UPDATE \
    --no-progress

rm -rf ARTIFACTS_BLOB_UPDATE

echo "Files uploaded"

expiretime=$(date -u -d '30 minutes' +%Y-%m-%dT%H:%MZ)
token=$(az storage container generate-sas --name $templateContainer --expiry $expiretime --permissions r --output tsv --connection-string $connection)
template_url="$(az storage blob url --container-name $templateContainer --name azuredeploy.json --output tsv --connection-string $connection)?$token"
blob_base_url="$(az storage account show -n $storageAccountName  --query primaryEndpoints.blob)"
container_base_url=${blob_base_url//\"/}$templateContainer

echo "# Deploy infrastructure"
echo "az deployment sub create -n $deployId ... "

# deploy infra
az deployment sub create -n $deployId  -l ${location} --template-uri $template_url \
    --parameters @"${azudeDeployParameters}"  \
    --parameters @"${spAzudeDeployParameters}"  \
    --parameters _artifactsLocation=$container_base_url   \
    --parameters _artifactsLocationSasToken="?$token" > ${deploymentOut}

function getOutput() {
  jq -r '.properties.outputs.'${1}'.value' ${deploymentOut}
}

# Enable HDInsight monitor
$(getOutput "hdInsightEnableMonitor")

./setup-aks.sh ${subscriptionName} ${deploymentOut}