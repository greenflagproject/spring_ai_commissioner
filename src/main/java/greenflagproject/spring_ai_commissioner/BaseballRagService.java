package greenflagproject.spring_ai_commissioner;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

@Service
public class BaseballRagService {

	private final ChatClient chatClient;
	private final VectorStore vectorStore;
	private final String systemPrompt = """
				あなたは野球団体のコミッショナーです。

				野球規則に準拠し、次のルールを守って回答してください。
				- 提供された参考資料を根拠として回答する。
				- 参考資料に書かれていない内容を断定しない。
				- 資料から判断できない場合は、その旨を明示する。
				- 条項番号や規則の内容を勝手に作らない。
				- 日本語で、根拠と結論を区別して説明する。
				""";

	public BaseballRagService(
			ChatClient.Builder chatClientBuilder,
			VectorStore vectorStore) {
		this.chatClient = chatClientBuilder.build();
		this.vectorStore = vectorStore;
	}

	public BaseballAiController.ChatAnswer ask(String question, String imagePath) {
		// Advisor(QuestionAnswerAdvisor)をChatClientに提供し、RAGを実現
		// 下記処理が内部で行っている処理
		// ①質問に関連するデータをVectorDBから取得する
		// ②取得した関連データと質問をLLMに渡し、LLMの推論結果を受け取る
		String answer =
				this.chatClient.prompt()
						.advisors(QuestionAnswerAdvisor.builder(vectorStore).build())
						.system(systemPrompt)
						.user(u -> {
							if (imagePath!=null ) {
								u.text(question).media(BaseballAiController.getMedia(imagePath));
							}
							else {
								u.text(question);
							}
						})
						.call()
						.content();

		// 推論結果とVector検索結果を回答する
		return new BaseballAiController.ChatAnswer(answer);
	}
}