/*
 * Copyright 2015-2016 OpenCB
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

package org.opencb.opencga.app.cli.main.options.catalog;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;
import org.opencb.opencga.app.cli.GeneralCliOptions.NumericOptions;

import static org.opencb.opencga.app.cli.GeneralCliOptions.CommonCommandOptions;
import static org.opencb.opencga.app.cli.GeneralCliOptions.DataModelOptions;

/**
 * Created by pfurio on 13/06/16.
 */
@Parameters(commandNames = {"users"}, commandDescription = "User commands")
public class UserCommandOptions {

    public CreateCommandOptions createCommandOptions;
    public InfoCommandOptions infoCommandOptions;
    public UpdateCommandOptions updateCommandOptions;
    public ChangePasswordCommandOptions changePasswordCommandOptions;
    public DeleteCommandOptions deleteCommandOptions;
    public ProjectsCommandOptions projectsCommandOptions;
    public LoginCommandOptions loginCommandOptions;
    public LogoutCommandOptions logoutCommandOptions;
    public ResetPasswordCommandOptions resetPasswordCommandOptions;

    public JCommander jCommander;
    public CommonCommandOptions commonCommandOptions;
    public DataModelOptions commonDataModelOptions;
    public NumericOptions commonNumericOptions;

    public UserCommandOptions(CommonCommandOptions commonCommandOptions, DataModelOptions dataModelOptions, NumericOptions numericOptions,
                              JCommander jCommander) {

        this.commonCommandOptions = commonCommandOptions;
        this.commonDataModelOptions = dataModelOptions;
        this.commonNumericOptions = numericOptions;
        this.jCommander = jCommander;

        this.createCommandOptions = new CreateCommandOptions();
        this.infoCommandOptions = new InfoCommandOptions();
        this.updateCommandOptions = new UpdateCommandOptions();
        this.changePasswordCommandOptions = new ChangePasswordCommandOptions();
        this.deleteCommandOptions = new DeleteCommandOptions();
        this.projectsCommandOptions = new ProjectsCommandOptions();
        this.loginCommandOptions = new LoginCommandOptions();
        this.logoutCommandOptions = new LogoutCommandOptions();
        this.resetPasswordCommandOptions = new ResetPasswordCommandOptions();
    }

    public JCommander getjCommander() {
        return jCommander;
    }

    public class BaseUserCommand {

        @ParametersDelegate
        public CommonCommandOptions commonOptions = commonCommandOptions;

        @Parameter(names = {"-u", "--user"}, description = "User id, this must be unique in this OpenCGA installation",  required = true, arity = 1)
        public String user;

    }

    @Parameters(commandNames = {"create"}, commandDescription = "Create a new user")
    public class CreateCommandOptions extends BaseUserCommand {

        @Parameter(names = {"-n", "name"}, description = "User name", required = true, arity = 1)
        public String name;

        @Parameter(names = {"-p", "--password"}, description = "User password", required = true,  password = true, arity = 1)
        public String password;

        @Parameter(names = {"-e", "--email"}, description = "User email", required = true, arity = 1)
        public String email;

        @Parameter(names = {"-o", "--organization"}, description = "User organization", required = false, arity = 1)
        public String organization;

        @Parameter(names = {"--project-name"}, description = "Project name. Default: Default", required = false, arity = 1)
        public String projectName;

        @Parameter(names = {"--project-alias"}, description = "Project alias: Default: default", required = false, arity = 1)
        public String projectAlias;

        @Parameter(names = {"--project-description"}, description = "Project description.", required = false, arity = 1)
        public String projectDescription;

        @Parameter(names = {"--project-organization"}, description = "Project organization", required = false, arity = 1)
        public String projectOrganization;

    }

    @Parameters(commandNames = {"info"}, commandDescription = "Get complete information of the user together with owned and shared projects"
            + " and studies")
    public class InfoCommandOptions {

        @ParametersDelegate
        public CommonCommandOptions commonOptions = commonCommandOptions;

        @ParametersDelegate
        public DataModelOptions dataModelOptions = commonDataModelOptions;

        @Deprecated
        @Parameter(names = {"--last-modified"}, description = "[DEPRECATED] If matches with the user's last activity, return " +
                "an empty QueryResult", arity = 1, required = false)
        public String lastModified;
    }

    @Parameters(commandNames = {"update"}, commandDescription = "Update some user attributes using GET method")
    public class UpdateCommandOptions extends BaseUserCommand {

        @Parameter(names = {"-n", "--name"}, description = "Name", arity = 1)
        public String name;

        @Parameter(names = {"-e", "--email"}, description = "Email", arity = 1)
        public String email;

        @Parameter(names = {"-o", "--organization"}, description = "Organization", arity = 1)
        public String organization;

        @Parameter(names = {"--attributes"}, description = "Attributes", arity = 1)
        public String attributes;

        @Parameter(names = {"--configs"}, description = "Configs", arity = 1)
        public String configs;

    }
    @Parameters(commandNames = {"change-password"}, commandDescription = "Update some user attributes using GET method")
    public class ChangePasswordCommandOptions {

        @Parameter(names = {"--password"}, description = "password", arity = 1, required = true)
        public String password;

        @Parameter(names = {"--npassword"}, description = "new password", arity = 1, required = true)
        public String npassword;
    }

    @Parameters(commandNames = {"delete"}, commandDescription = "Delete an user [NO TESTED]")
    public class DeleteCommandOptions extends BaseUserCommand {

    }

    @Parameters(commandNames = {"projects"}, commandDescription = "List all projects and studies belonging to the selected user")
    public class ProjectsCommandOptions {

        @ParametersDelegate
        public CommonCommandOptions commonOptions = commonCommandOptions;

        @ParametersDelegate
        public DataModelOptions dataModelOptions = commonDataModelOptions;

        @ParametersDelegate
        public NumericOptions numericOptions = commonNumericOptions;

        @Parameter(names = {"-u", "--user"}, description = "User id, this must be unique in this OpenCGA installation",  required = false, arity = 1)
        public String user;

        @Parameter(names = {"--shared"}, description = "Show only the projects and studies shared with the user.", arity = 0)
        public boolean shared;

    }

    @Parameters(commandNames = {"login"}, commandDescription = "Login as a user")
    public class LoginCommandOptions extends BaseUserCommand {

        @Parameter(names = {"-p", "--password"}, description = "Password", arity = 1, required = true, password = true)
        public String password;

    }

    @Parameters(commandNames = {"logout"}, commandDescription = "End user session")
    public class LogoutCommandOptions {

        @Parameter(names = {"-S", "--session-id"}, description = "SessionId", required = false, arity = 1, hidden = true)
        public String sessionId;

    }

    @Parameters(commandNames = {"reset-password"}, commandDescription = "Reset password")
    public class ResetPasswordCommandOptions extends BaseUserCommand {

    }

}
