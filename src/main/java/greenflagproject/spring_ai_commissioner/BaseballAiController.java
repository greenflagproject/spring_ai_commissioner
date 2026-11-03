package greenflagproject.spring_ai_commissioner;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/baseball")
public class BaseballAiController {

	private final ChatClient chatClient;

	private final BaseballRagService ragService;

	private final org.springframework.ai.tool.ToolCallbackProvider baseballMcpToolProvider;

	public BaseballAiController(
			ChatClient.Builder chatClientBuilder,
			BaseballRagService ragService,
			org.springframework.ai.tool.ToolCallbackProvider baseballMcpToolProvider) {
		this.chatClient = chatClientBuilder.build();
		this.ragService = ragService;
		this.baseballMcpToolProvider = baseballMcpToolProvider;
	}

	public record AskRequest(String question, String imagePath) {
	}

	public record ChatAnswer(String answer) {
	}

	@PostMapping("/rag")
	public ChatAnswer rag(@RequestBody AskRequest request) {

		if (request.question() == null
				|| request.question().isBlank()) {
			throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST,
					"質問を入力してください。"
			);
		}

		return ragService.ask(request.question(), request.imagePath());
	}

	@PostMapping("/mcp")
	public ChatAnswer mcp(@RequestBody AskRequest request) {

		if (request.question() == null
				|| request.question().isBlank()) {
			throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST,
					"質問を入力してください。"
			);
		}

		var question = request.question();
		String imagePath = request.imagePath();
		String response = chatClient
				.prompt()
				.user(u -> {
					if (imagePath == null) {
						u.text(question);
					} else {
						u.text(question).media(getMedia(imagePath));
					}
				})
				.tools(baseballMcpToolProvider)
				.call()
				.content();
//		System.out.println(response);
//		String response="質問: %s".formatted(request.question());

		return new BaseballAiController.ChatAnswer(response);
	}

	public static Media getMedia(String filePathString) {
		try{
			Path path = Paths.get(filePathString);
			Resource imageResource = new FileSystemResource(path.toFile());

			String contentType = Files.probeContentType(path);
			MediaType mediaType = (contentType != null) ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM;
			return new Media(mediaType, imageResource);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
