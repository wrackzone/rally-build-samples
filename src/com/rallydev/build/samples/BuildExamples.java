/**
 * 
 * @author Barry Mullan, Senior Product & Platform Specialist, Rally Software (2014)
 * 
 * An example of creating build results in Rally using the java api. For more information on the Java SDK for Rally see
 * https://github.com/RallyTools/RallyRestToolkitForJava/wiki/User-Guide#setup
 * 
 * API Documents can be found here
 * http://rallytools.github.io/RallyRestToolkitForJava/
 *
 * A build object instance is represented in Rally as a green/red indicator in the top left of all Rally pages when 
 * the particular project is selected. 
 * 
 * In addition build objects can be reported on using the Build dashboard reports in the Rally App Catalog
 * See https://help.rallydev.com/developer-tool-apps for more information.
 * 
 */

package com.rallydev.build.samples;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Properties;

import com.google.gson.JsonObject;
import com.rallydev.rest.RallyRestApi;
import com.rallydev.rest.request.CreateRequest;
import com.rallydev.rest.request.QueryRequest;
import com.rallydev.rest.request.UpdateRequest;
import com.rallydev.rest.response.CreateResponse;
import com.rallydev.rest.response.QueryResponse;
import com.rallydev.rest.response.UpdateResponse;
import com.rallydev.rest.util.Fetch;
import com.rallydev.rest.util.QueryFilter;

public class BuildExamples {
	
	private RallyRestApi restApi = null;
	private JsonObject workspace = null;
	
	/**
	 * 
	 * @param server
	 * @param userName
	 * @param passWord
	 * @param workspaceName
	 * @throws Exception
	 */
	
	BuildExamples(String server, String userName, String passWord, String workspaceName) throws Exception {
		restApi = new RallyRestApi(new URI(server), userName, passWord);
		restApi.setApplicationName("BuildSamples");
		this.workspace = queryWorkspace(workspaceName);
	}
	
	
	/**
	 * 
	 * @param name
	 * @return
	 * @throws Exception 
	 */
	public JsonObject queryWorkspace(String name) throws Exception {
		QueryRequest workspaces = new QueryRequest("workspace");
		workspaces.setFetch(new Fetch("Name"));
		workspaces.setQueryFilter(new QueryFilter("Name", "=", name));
		workspaces.setPageSize(5);
        workspaces.setLimit(5);
        
        QueryResponse queryResponse = restApi.query(workspaces);
        if (queryResponse.wasSuccessful()) {
        	if ( queryResponse.getTotalResultCount()>0)
        		return queryResponse.getResults().get(0).getAsJsonObject();
        }
        throw new Exception("Workspace not found! " + name);
	}
	
	/**
	 * Returns a project object from it's name
	 * 
	 * @param name
	 * @return
	 * @throws IOException 
	 * 
	 */
	public JsonObject queryProject(String name) throws IOException {
		
		QueryRequest projects = new QueryRequest("project");
		projects.setWorkspace(this.workspace.get("_ref").getAsString());
		projects.setFetch(new Fetch("Name"));
		projects.setQueryFilter( new QueryFilter("Name", "=", name) );
		projects.setPageSize(5);
        projects.setLimit(5);
        
        QueryResponse queryResponse = restApi.query(projects);
        if (queryResponse.wasSuccessful()) {
        	// get the first result
        	if (queryResponse.getResults().size()>0)
        		return queryResponse.getResults().get(0).getAsJsonObject();
        }
        return null;
	}
	
	/**
	 * Returns a project object from it's name
	 * 
	 * @param name
	 * @return
	 * @throws IOException 
	 * 
	 */
	public JsonObject queryBuildDefinition(String name, JsonObject project) throws IOException {
		
		QueryRequest builddefinitions = new QueryRequest("builddefinition");
		builddefinitions.setWorkspace(this.workspace.get("_ref").getAsString());
		builddefinitions.setFetch(new Fetch("Name"));
		builddefinitions.setQueryFilter(QueryFilter.and(
				new QueryFilter("Name", "=", name),
				new QueryFilter("Project", "=", project.get("_ref").getAsString())
				));
		builddefinitions.setPageSize(5);
        builddefinitions.setLimit(5);
        
        QueryResponse queryResponse = restApi.query(builddefinitions);
        if (queryResponse.wasSuccessful()) {
        	// get the first result
        	if ( queryResponse.getResults().size()>0)
        		return queryResponse.getResults().get(0).getAsJsonObject();
        }
        return null;
	}
	
	public JsonObject queryStoryById( String id ) throws IOException {
		
		QueryRequest storyQuery = new QueryRequest("hierarchicalrequirement");
		storyQuery.setWorkspace(this.workspace.get("_ref").getAsString());
		storyQuery.setFetch(new Fetch("Name","c_DeploymentKanban"));
		storyQuery.setQueryFilter(
				new QueryFilter("FormattedID", "=", id)
		);
		storyQuery.setPageSize(5);
        storyQuery.setLimit(5);
        
        QueryResponse queryResponse = restApi.query(storyQuery);
        if (queryResponse.wasSuccessful()) {
        	// get the first result
        	if ( queryResponse.getResults().size()>0)
        		return queryResponse.getResults().get(0).getAsJsonObject();
        }
        return null;
		
	}
	
	public UpdateResponse updateKanbanState( JsonObject story, String newState) throws IOException {
		JsonObject updatedStory = new JsonObject();
		updatedStory.addProperty("c_DeploymentKanban", newState);
		UpdateRequest updateRequest = new UpdateRequest(story.get("_ref").getAsString(), updatedStory);
		UpdateResponse updateResponse = restApi.update(updateRequest);
		return updateResponse;
	}
	
	/**
	 * Creates a build definition; a build definition is required to record build results against.
	 * 
	 * @param name
	 * @param description
	 * @param project
	 * @return
	 * @throws IOException 
	 * 
	 */
	public JsonObject createBuildDefinition(String name, String description, JsonObject project) throws IOException {
		
		JsonObject buildDefinition = new JsonObject();
		buildDefinition.addProperty("Name", name);
		buildDefinition.addProperty("Description", description);
		buildDefinition.addProperty("Project", project.get("_ref").getAsString());
		buildDefinition.addProperty("Workspace", this.workspace.get("_ref").getAsString());
		
		CreateRequest createRequest = new CreateRequest("builddefinition", buildDefinition);
		CreateResponse createResponse = restApi.create(createRequest);
		System.out.println(String.format("Created %s", createResponse.getObject().get("_ref").getAsString()));

		return buildDefinition;
	}
	
	
	/**
	 * 
	 * @param buildDefinition
	 * @param duration
	 * @param message
	 * @param number
	 * @param status
	 * @param uri
	 * @return
	 * @throws IOException 
	 */
	public JsonObject createBuild( JsonObject buildDefinition, double duration, String message, String number, String status, String uri ) throws IOException {
	
		JsonObject build = new JsonObject();
		build.addProperty("BuildDefinition", buildDefinition.get("_ref").getAsString());
		build.addProperty("Duration", duration);
		build.addProperty("Message", message);
		build.addProperty("Number", number);
		build.addProperty("Status", status);
		build.addProperty("Uri", uri);
		CreateRequest createRequest = new CreateRequest("build", build);
		CreateResponse createResponse = restApi.create(createRequest);
		if (createResponse.getErrors().length > 0) {
			System.out.println( createResponse.getErrors()[0]);
			return null;
		}
			
		System.out.println(String.format("Created %s", createResponse.getObject().get("_ref").getAsString()));
		
		return build;
	}

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		
		Properties prop = new Properties();
		InputStream input = new FileInputStream("config.properties");
		prop.load(input);

		String server = "https://rally1.rallydev.com";
		String workspace = "DBS";

		String username = prop.getProperty("user");
		String password = prop.getProperty("password");
		
		BuildExamples sample = new BuildExamples(server, username, password, workspace);
		
		// first query the project object (builddefinitions and builds are associated with a specific project)
		JsonObject project = sample.queryProject("Private Banking");
		
		// then try to query for the build definition
		JsonObject buildDefinition = sample.queryBuildDefinition("Private Banking", project);
		
		// if the build definition does not exist we will create it
		if (buildDefinition==null) {
			System.out.println("Build not found!");
			// create it
			buildDefinition = sample.createBuildDefinition("Build 1", "A sample build definition for Team 1", project);
		} else {
			// finally, create a build object associated to the build definition
			System.out.println( buildDefinition.get("Name"));
			sample.createBuild(buildDefinition, 1, "A failing build", "99", "FAILURE", "");
//			sample.createBuild(buildDefinition, 10, "A passing build", "100", "SUCCESS", "");
		}
		
		// Query story example 
		JsonObject story = sample.queryStoryById("US1");
		
		if (story != null) {
			System.out.println("Story:"+story.get("Name")+" State:"+story.get("c_DeploymentKanban"));
			
			// update the state to the first state
			UpdateResponse response = sample.updateKanbanState(story, "Defined");
			System.out.println("Success:"+response.wasSuccessful());
			// update the state to the second state
			response = sample.updateKanbanState(story, "In Dev");
			System.out.println("Success:"+response.wasSuccessful());
			
		}
		
	}

}
