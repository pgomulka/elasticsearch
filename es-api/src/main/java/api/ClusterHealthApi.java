package api;

import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.servers.ServerVariable;
import io.swagger.v3.oas.annotations.tags.Tag;
//import org.elasticsearch.action.admin.cluster.health.ClusterHealthAction;
//import org.elasticsearch.client.node.NodeClient;
//import org.elasticsearch.rest.BaseRestHandler;
//import org.elasticsearch.rest.RestRequest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.io.IOException;

/**
 * Test
 */

public class ClusterHealthApi {

    @GET
    @Path("/{username}")
    @Operation(summary = "Get user by user name",
        responses = {
            @ApiResponse(description = "The user",
                content = @Content(mediaType = "application/json"/*,
                    schema = @Schema(implementation = ClusterHealthAction.class)*/)),
            @ApiResponse(responseCode = "400", description = "User not found")})
   public void prepareRequest(/*final RestRequest request, final NodeClient client*/) throws IOException{

    }

}
