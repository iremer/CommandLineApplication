GitHub Command Line Java Application

This command-line Java application allows users to fetch data from GitHub using the GitHub REST API. It finds the most forked public repositories of a given organization and retrieves information about the top contributors of these repositories.


Overview

The application is designed to accomplish the following tasks:

- Find the most forked public repositories of a given organization.
- Retrieve details: Repository name, fork quantity, public URL, and description.
- Identify the top contributors of these repositories.
- Retrieve details: Repository name, username, contribution quantity, and user’s follower quantity.

It generates two CSV files:

- <organization_name>_repos.csv: Contains columns for repo, forks, URL, and description.
- <organization_name>_users.csv: Contains columns for repo, username, contributions, and followers.

Usage
To compile the application, navigate to the src directory and execute the following command:

"javac -cp path/to/gson.jar Main.java"

To run the application, use the following command with appropriate parameters:

"java -cp path/to/gson.jar Main.java param1 param2 param3"

param1: Name of the organization.
param2: Number of most forked repositories of organization.
param3: Number of top contributors of repository.

Don't forget to replace path/to/gson.jar with the actual path to the gson library (gson-2.10.1.jar) included in the project directory.