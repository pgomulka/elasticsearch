import java.io.IOException;

public class ClusterHealthApi {
    @GET
    @Path("/{username}")
    @Operation(summary = "Get user by user name",
        responses = {
            @ApiResponse(description = "The user",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = User.class))),
            @ApiResponse(responseCode = "400", description = "User not found")})
    public RestChannelConsumer prepareRequest(final RestRequest request, final NodeClient client) throws IOException {

    }
