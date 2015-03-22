
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class PhpPagesSize {

	private void analyzeTimeOutPagesSize(String path) {
		try {
			BufferedReader in = new BufferedReader(new FileReader(path));
			String str, urlPage = "";
			int otherPages = 0, imagePages = 0, cssFiles = 0, noFoundPages=0;

			while ((str = in.readLine()) != null) {

				if (str.contains("/index.php?")) {
					String timeoutValue;

					if (str.contains(" - POST")) {
						
						timeoutValue = str.substring(
								str.indexOf("POST - ") + 6,
								str.indexOf("POST - ") + 6 + 4);
					} else {
						timeoutValue = str.substring(str.indexOf("GET - ") + 6,
								str.indexOf("GET - ") + 6 + 4);

					}

					timeoutValue = timeoutValue.replace("-", "");
					int timeout = Integer.parseInt(timeoutValue.trim());

					if (timeout > 710) {
						urlPage = str.substring(str.indexOf("/index.php?"),
								str.length());
						if (urlPage.contains("title=Image:"))
							imagePages++;
						else if (urlPage.contains("title=MediaWiki:Common.css")
								|| urlPage
										.contains("title=MediaWiki:Monobook.css"))
							cssFiles++;
						else
							otherPages++;

						try {

							HttpURLConnection conn = (HttpURLConnection) new URL(
									"http://10.143.42.7" + urlPage)
									.openConnection();
							InputStream input = conn.getInputStream();

							// long size = input.available();
							// System.out.println("PAGE "+urlPage+"  SIZE: "+size);

							BufferedReader buffer = new BufferedReader(
									new InputStreamReader(input));
							String aux = "", pageText = "";
							while ((aux = buffer.readLine()) != null) {
								pageText = pageText + aux;
							}

							System.out.println("PAGE " + urlPage + " TIMEOUT: "+timeout+" SIZE: "+ pageText.getBytes().length);

						} catch (MalformedURLException e) {
							// TODO Auto-generated catch block
							System.out.println(">> ERROR URL not Found "+e.getMessage());
							
							noFoundPages++;
						} catch (IOException e) {
							// TODO Auto-generated catch block
							System.out.println(">> ERROR URL not Found "+e.getMessage());
							noFoundPages++;
						}

					}
				}
			}

			System.out.println("---------------------------------------------");
			System.out.println("Number of image pages: " + imagePages);
			System.out.println("Number of css files: " + cssFiles);
			System.out.println("Number of other pages: " + otherPages);
			System.out.println("Number of not found pages: " + noFoundPages);
			System.out.println("---------------------------------------------");

		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void main(String args[]) {

		PhpPagesSize main = new PhpPagesSize();

		main.analyzeTimeOutPagesSize("./wikijector/log.txt");

	}
}
