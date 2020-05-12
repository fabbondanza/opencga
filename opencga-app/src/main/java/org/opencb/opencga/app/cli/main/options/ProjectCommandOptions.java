/*
 * Copyright 2015-2017 OpenCB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opencb.opencga.app.cli.main.options;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;

import static org.opencb.opencga.app.cli.GeneralCliOptions.*;

/**
 * Created by pfurio on 13/06/16.
 */
@Parameters(commandNames = {"projects"}, commandDescription = "Project commands")
public class ProjectCommandOptions {

    public CreateCommandOptions createCommandOptions;
    public InfoCommandOptions infoCommandOptions;
    public SearchCommandOptions searchCommandOptions;
    public StudiesCommandOptions studiesCommandOptions;
    public UpdateCommandOptions updateCommandOptions;

    public JCommander jCommander;
    public CommonCommandOptions commonCommandOptions;
    public DataModelOptions commonDataModelOptions;
    public NumericOptions commonNumericOptions;

    public ProjectCommandOptions(CommonCommandOptions commonCommandOptions, DataModelOptions dataModelOptions, NumericOptions numericOptions,
                                 JCommander jCommander) {

        this.commonCommandOptions = commonCommandOptions;
        this.commonDataModelOptions = dataModelOptions;
        this.commonNumericOptions = numericOptions;
        this.jCommander = jCommander;

        this.createCommandOptions = new CreateCommandOptions();
        this.infoCommandOptions = new InfoCommandOptions();
        this.searchCommandOptions = new SearchCommandOptions();
        this.studiesCommandOptions = new StudiesCommandOptions();
        this.updateCommandOptions = new UpdateCommandOptions();
    }

    public class BaseProjectCommand {

        @ParametersDelegate
        public CommonCommandOptions commonOptions = commonCommandOptions;

        @Parameter(names = {"--project"}, description = "Project id", required = true, arity = 1)
        public String project;
    }

    @Parameters(commandNames = {"create"}, commandDescription = "Create a new project")
    public class CreateCommandOptions {

        @ParametersDelegate
        public CommonCommandOptions commonOptions = commonCommandOptions;

        @Parameter(names = {"--id"}, description = "Project id, this must be unique for the user", required = true, arity = 1)
        public String id;

        @Parameter(names = {"-n", "--name"}, description = "Project name", required = true, arity = 1)
        public String name;

        @Parameter(names = {"-d", "--description"}, description = "A brief description of the content of this project", arity = 1)
        public String description;

        @Parameter(names = {"--organism-scientific-name"}, description = "Organism scientific name, e.g. Homo sapiens", required = true, arity = 1)
        public String scientificName;

        @Parameter(names = {"--organism-common-name"}, description = "Organism common name", arity = 1)
        public String commonName;

        @Parameter(names = {"--organism-assembly"}, description = "Organism assembly, e.g. GRCh38", required = true, arity = 1)
        public String assembly;
    }

    @Parameters(commandNames = {"info"}, commandDescription = "Get project information")
    public class InfoCommandOptions extends BaseProjectCommand {

        @ParametersDelegate
        public DataModelOptions dataModelOptions = commonDataModelOptions;

    }

    @Parameters(commandNames = {"search"}, commandDescription = "Search projects")
    public class SearchCommandOptions {

        @ParametersDelegate
        public CommonCommandOptions commonOptions = commonCommandOptions;

        @ParametersDelegate
        public DataModelOptions dataModelOptions = commonDataModelOptions;

        @Parameter(names = {"--owner"}, description = "Owner of the project", arity = 1)
        public String owner;

        @Parameter(names = {"-n", "--name"}, description = "Project name.", arity = 1)
        public String name;

        @Parameter(names = {"--fqn"}, description = "Project fully qualified name.", arity = 1)
        public String fqn;

        @Parameter(names = {"--organization"}, description = "Project organization.", arity = 1)
        public String organization;

        @Parameter(names = {"--description"}, description = "Project description", arity = 1)
        public String description;

        @Parameter(names = {"-s", "--study"}, description = "Study id", arity = 1)
        public String study;

        @Parameter(names = {"--creation-date"}, description = "Creation date. Format: yyyyMMddHHmmss. Examples: >2018, 2017-2018, <201805",
                arity = 1)
        public String creationDate;

        @Parameter(names = {"--modification-date"}, description = "Modification date. Format: yyyyMMddHHmmss. Examples: >2018, 2017-2018,"
                + " <201805", arity = 1)
        public String modificationDate;

        @Parameter(names = {"--status"}, description = "Status", arity = 1)
        public String status;

        @Parameter(names = {"--attributes"}, description = "Attributes.", arity = 1)
        public String attributes;
    }

    @Parameters(commandNames = {"studies"}, commandDescription = "Get all studies from a project")
    public class StudiesCommandOptions extends BaseProjectCommand {

        @ParametersDelegate
        public DataModelOptions dataModelOptions = commonDataModelOptions;

        @ParametersDelegate
        public NumericOptions numericOptions = commonNumericOptions;

    }

    @Parameters(commandNames = {"update"}, commandDescription = "Update a project")
    public class UpdateCommandOptions extends BaseProjectCommand {

        @Parameter(names = {"-n", "--name"}, description = "Project name", arity = 1)
        public String name;

        @Parameter(names = {"-d", "--description"}, description = "Description", arity = 1)
        public String description;

        @Parameter(names = {"--organism-common-name"}, description = "Organism common name", arity = 1)
        public String commonName;

        @Parameter(names = {"--organism-scientific-name"}, description = "Organism scientific name", arity = 1)
        public String scientificName;

        @Parameter(names = {"--organism-assembly"}, description = "Organism assembly", arity = 1)
        public String assembly;
    }

}
