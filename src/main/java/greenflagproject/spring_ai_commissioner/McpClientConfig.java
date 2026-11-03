package greenflagproject.spring_ai_commissioner;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpClientConfig {

	@Bean
	public McpSyncClient baseballMcpClient(
			@Value("${baseball.mcp.server.url:http://localhost/mcp}") String serverUrl) {
		return McpClient.sync(HttpClientStreamableHttpTransport.builder(serverUrl).build())
				.clientInfo(McpSchema.Implementation.builder("baseball-mcp-client", "1.0.0").build())
				.build();
	}

	@Bean
	public ToolCallbackProvider baseballMcpToolProvider(McpSyncClient baseballMcpClient) {
		return SyncMcpToolCallbackProvider.builder()
				.mcpClients(baseballMcpClient)
				.toolFilter((client, tool) -> true)
				.build();
	}
}
