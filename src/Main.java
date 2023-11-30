import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Please provide three parameters for organization name, " +
                    "number of most forked repositories and top contributors of these repositories respectively.");
        } else {

            try {
                // Followed naming conventions as recommended in
                // https://www.oracle.com/java/technologies/javase/codeconventions-namingconventions.html

                String organizationName = args[0];
                int mostForkedRepositoriesCount = Integer.parseInt(args[1]);
                int topContributorsCount = Integer.parseInt(args[2]);

                // Retrieve repositories of organization
                URL organizationReposUrl = new URL("https://api.github.com/orgs/" + organizationName + "/repos");
                HttpURLConnection repoConnection = (HttpURLConnection) organizationReposUrl.openConnection();
                repoConnection.setRequestMethod("GET");

                if (repoConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    String reposResponse = convertToString(repoConnection.getInputStream());

                    Gson gson = new Gson();
                    JsonArray reposJsonArray = gson.fromJson(reposResponse, JsonArray.class);

                    Repo[] reposList = getRepos(reposJsonArray);
                    Arrays.sort(reposList, Comparator.comparingInt(Repo::getForksCount).reversed());

                    String organizationFileName = organizationName + "_repos.csv";
                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(organizationFileName))) {
                        writer.write("Repo;Forks;URL;Description");
                        writer.newLine();

                        HttpURLConnection contributorsConnection = null;

                        for (int i = 0; i < mostForkedRepositoriesCount; i++) {
                            Repo repo = reposList[i];
                            writer.write(String.format("%s;%d;%s;%s",
                                    repo.getRepoName(), repo.getForksCount(), repo.getRepoUrl(), repo.getDescription()));
                            writer.newLine();

                            // Retrieve contributors of repository
                            URL contributorsOfRepoUrl = new URL("https://api.github.com/repos/" + organizationName + "/"
                                    + repo.getRepoName() + "/contributors");
                            contributorsConnection = (HttpURLConnection) contributorsOfRepoUrl.openConnection();
                            contributorsConnection.setRequestMethod("GET");

                            if (contributorsConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                                String contributorsResponse = convertToString(contributorsConnection.getInputStream());

                                JsonArray contributorsJsonArray = gson.fromJson(contributorsResponse, JsonArray.class);

                                // Returns the list in decreasing order according to contributions by default
                                Contributor[] contributorsList = getContributors(contributorsJsonArray, repo.getRepoName());

                                String contributorsFileName = organizationName + "_users.csv";
                                boolean fileExists = new File(contributorsFileName).exists();

                                // Checks file existence; appends lines if present, or creates and appends lines if not
                                try (BufferedWriter writer2 = new BufferedWriter(new FileWriter(contributorsFileName, true))) {
                                    if (!fileExists) {
                                        writer2.write("Repo;Username;Contributions;Followers");
                                        writer2.newLine();
                                    }

                                    for (int j = 0; j < topContributorsCount; j++) {
                                        Contributor contributor = contributorsList[j];

                                        HttpURLConnection followersConnection;

                                        int followersCount = 0, page = 1;
                                        while (true) {
                                            // Retrieve followers of contributor
                                            URL followersUrl = new URL("https://api.github.com/users/" + contributor.getUsername() +
                                                    "/followers?page=" + page + "&per_page=100");
                                            followersConnection = (HttpURLConnection) followersUrl.openConnection();
                                            followersConnection.setRequestMethod("GET");

                                            if (followersConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                                                String followersResponse = convertToString(followersConnection.getInputStream());
                                                JsonArray followersJsonArray = gson.fromJson(followersResponse, JsonArray.class);

                                                int pageCount = followersJsonArray.size();
                                                if (pageCount == 0) {
                                                    break; // No more pages left
                                                }
                                                followersCount += pageCount;
                                                page++;
                                            }
                                        }
                                        followersConnection.disconnect();

                                        writer2.write(String.format("%s;%s;%d;%s",
                                                repo.getRepoName(), contributor.getUsername(), contributor.getContributions(), followersCount));
                                        writer2.newLine();
                                    }
                                }
                            } else if (contributorsConnection.getResponseCode() == HttpURLConnection.HTTP_NO_CONTENT) {
                                System.out.println("No results found from the API request.");
                            } else if (contributorsConnection.getResponseCode() == HttpURLConnection.HTTP_FORBIDDEN) {
                                System.out.println("Access denied.");
                            } else {
                                System.out.println("Resource not found.");
                            }
                        }
                        if (contributorsConnection != null) contributorsConnection.disconnect();

                    } catch (Exception e) {
                        System.out.println("Failed to write data to csv.");
                    }

                } else if (repoConnection.getResponseCode() == HttpURLConnection.HTTP_NO_CONTENT) {
                    System.out.println("No results found from the API request.");
                } else if (repoConnection.getResponseCode() == HttpURLConnection.HTTP_FORBIDDEN) {
                    System.out.println("Access denied.");
                } else {
                    System.out.println("Resource not found.");
                }

                repoConnection.disconnect();

            } catch (NumberFormatException exception) {
                // In case the user enters input in a format that can not be converted into int
                System.out.println("The last two parameters must be numbers!");
            } catch (IOException e) {
                System.out.println("There has been an issue while reaching GitHub REST API");
            }
        }
    }

    // Converts InputStream to String for JSON operations
    public static String convertToString(InputStream inputStream) {
        // Read entire input as single token
        Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8).useDelimiter("\\A");
        return scanner.hasNext() ? scanner.next() : "";
    }

    // Parses a JSON array to construct an array of Repo objects
    private static Repo[] getRepos(JsonArray jsonArray) {
        Repo[] list = new Repo[jsonArray.size()];

        for (int i = 0; i < list.length; i++) {
            JsonObject object = jsonArray.get(i).getAsJsonObject();
            Repo repo = new Repo(
                    object.get("name").getAsString(),
                    object.get("forks_count").getAsInt(),
                    object.get("html_url").getAsString(),
                    object.get("description").getAsString()
            );
            list[i] = repo;
        }
        return list;
    }

    // Parses a JSON array to construct an array of Contributor objects
    private static Contributor[] getContributors(JsonArray jsonArray, String repoName) {
        Contributor[] list = new Contributor[jsonArray.size()];

        for (int i = 0; i < list.length; i++) {
            JsonObject object = jsonArray.get(i).getAsJsonObject();
            Contributor contributor = new Contributor(
                    repoName,
                    object.get("login").getAsString(),
                    object.get("contributions").getAsInt(),
                    object.get("followers_url").getAsString()
            );
            list[i] = contributor;
        }
        return list;
    }

    static class Repo {
        private String repoName;
        private int forksCount;
        private String repoUrl;
        private String description;

        public Repo(String repoName, int forksCount, String repoUrl, String description) {
            this.repoName = repoName;
            this.forksCount = forksCount;
            this.repoUrl = repoUrl;
            this.description = description;
        }

        public void setRepoName(String repoName) {
            this.repoName = repoName;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public void setRepoUrl(String repoUrl) {
            this.repoUrl = repoUrl;
        }

        public void setForksCount(int forksCount) {
            this.forksCount = forksCount;
        }

        public String getRepoName() {
            return repoName;
        }

        public String getDescription() {
            return description;
        }

        public String getRepoUrl() {
            return repoUrl;
        }

        public int getForksCount() {
            return forksCount;
        }
    }

    static class Contributor {
        private String repoName;
        private String username;
        private int contributions;
        private String followersUrl;

        public Contributor(String repoName, String username, int contributions, String followersUrl) {
            this.repoName = repoName;
            this.username = username;
            this.contributions = contributions;
            this.followersUrl = followersUrl;
        }

        public void setRepoName(String repoName) {
            this.repoName = repoName;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public void setContributions(int contributions) {
            this.contributions = contributions;
        }

        public void setFollowersUrl(String followersUrl) {
            this.followersUrl = followersUrl;
        }

        public String getRepoName() {
            return repoName;
        }

        public String getUsername() {
            return username;
        }

        public int getContributions() {
            return contributions;
        }

        public String getFollowersUrl() {
            return followersUrl;
        }
    }
}