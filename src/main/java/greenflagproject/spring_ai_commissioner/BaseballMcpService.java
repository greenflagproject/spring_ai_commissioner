package greenflagproject.spring_ai_commissioner;

import org.springframework.ai.document.Document;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class BaseballMcpService {
	private final VectorStore vectorStore;

	public BaseballMcpService(VectorStore vectorStore) {
		this.vectorStore = vectorStore;
	}

	@McpTool(description = "公認野球規則の条文を返します")
	public String getBaseballRules(
			@McpToolParam(description="公認野球規則のVectorDBを検索するためのキーワードとなる文章",
			required=true) String vectorSearchContext){

		// 質問のベクトル値作成とベクトル検索条件付与(閾値0.5, 上位5件)・・・(③)
		SearchRequest searchRequest = SearchRequest.builder()
				.query(vectorSearchContext)			//VectorDBを検索する文
//				.topK(5)					        //ベクトル検索条件 上位5件を返す
//				.similarityThreshold(0.5)	        //ベクトル検索条件 閾値0.5
				.build();

		// ベクトル検索実行し、検索結果を受け取る・・・(③④)
		List<Document> documents = vectorStore.similaritySearch(searchRequest);

		// 関連する規則が見つからない場合は、固定の回答を作って処理を終了する。
		if (documents.isEmpty()) {
			return "該当する規則を検索できませんでした。"
					+ "質問の表現を変えて再度お試しください。";
		}

		// 検索結果をLLMに渡す文字列に変換する・・・(⑤)
		String context = documents.stream()
				.map(this::formatDocument)
				.reduce((a, b) -> a + "\n\n---\n\n" + b)
				.orElse("");
		return context;
	}

	private String formatDocument(Document document) {
		Map<String, Object> metadata = document.getMetadata();

		return """
                セクション: %s
                条項ラベル: %s
                タイトル: %s
                PDFページ: %s
                本文:
                %s
                """.formatted(
				metadataValue(metadata, "section"),
				metadataValue(metadata, "label"),
				metadataValue(metadata, "title"),
				metadataValue(metadata, "pdf_page"),
				document.getText()
		);
	}

	private Source toSource(Document document) {
		Map<String, Object> metadata = document.getMetadata();

		return new Source(
				metadataValue(metadata, "section"),
				metadataValue(metadata, "label"),
				metadataValue(metadata, "title"),
				metadataValue(metadata, "pdf_page"),
				metadataValue(metadata, "printed_page_number"),
				document.getText()
		);
	}

	private String metadataValue(
			Map<String, Object> metadata,
			String key) {

		Object value = metadata.get(key);
		return value == null ? "" : value.toString();
	}

	public record Source(
			String section,
			String label,
			String title,
			String pdfPage,
			String printedPageNumber,
			String content) {
	}
}