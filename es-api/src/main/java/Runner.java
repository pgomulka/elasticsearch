import io.swagger.v3.jaxrs2.integration.SwaggerLoader;
import org.elasticsearch.rest.action.admin.indices.RestCreateIndexAction;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class Runner {
    public static void main(String[] args) throws Exception {
//        RestCreateIndexAction rest = new RestCreateIndexAction();
        SwaggerLoader swaggerLoader  = new SwaggerLoader();
        swaggerLoader.setResourcePackages("api,org.elasticsearch.rest.action.admin.indices");
        swaggerLoader.setResourceClasses(/*api.ClusterHealthApi,*/"org.elasticsearch.rest.action.admin.indices.RestCreateIndexAction");
        swaggerLoader.setPrettyPrint(true);
        swaggerLoader.setOutputFormat("JSON");
        Map<String, String> specs = swaggerLoader.resolve();

        String outputPath = "api-output";
        String outputFileName = "es-api";
        String encoding = "UTF-8";

        Path path = Paths.get(outputPath, "temp");
        final File parentFile = path.toFile().getParentFile();
        if (parentFile != null) {
            parentFile.mkdirs();
        }
        if (specs.get("JSON") != null) {
            path = Paths.get(outputPath, outputFileName + ".json");
            Files.write(path, specs.get("JSON").getBytes(Charset.forName(encoding)));
        }
        if (specs.get("YAML") != null) {
            path = Paths.get(outputPath, outputFileName + ".yaml");
            Files.write(path, specs.get("YAML").getBytes(Charset.forName(encoding)));
        }    }
}
