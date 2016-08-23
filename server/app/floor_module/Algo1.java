package floor_module;

import java.util.ArrayList;
import java.util.HashMap;

import com.fasterxml.jackson.databind.JsonNode;

public class Algo1 implements IAlgo {
	final double a = 10;
	final double b = 10;
	final int l1 = 10;

	HashMap<String, Wifi> input = new HashMap<String, Wifi>();
	ArrayList<Score> mostSimilar = new ArrayList<Score>(10);

	public Algo1(JsonNode json) throws Exception {
		JsonNode listenList = json.get("wifi");
		if (!listenList.isArray()) {
			throw new Exception("Wifi parameter is not array");
		}
		for (JsonNode listenObject : listenList) {
			JsonNode Jmac = listenObject.get("MAC");
			JsonNode Jrss = listenObject.get("rss");

			if (Jmac == null || Jrss == null) {
				throw new Exception("Invalid array wifi:: require mac,rss");
			}

			String mac = Jmac.textValue();
			int rss = Jrss.asInt();
			input.put(mac, new Wifi(mac, rss));
		}
	}

	private double compare(ArrayList<JsonNode> bucket) {

		long score = 0;
		int nNCM = 0;
		int nCM = 0;

		for (JsonNode wifiDatabase : bucket) {
			String mac = wifiDatabase.get("MAC").textValue();

			if (input.containsKey(mac)) {
				Integer diff = (Integer.parseInt(wifiDatabase.get("rss")
						.textValue()) - input.get(mac).rss);
				score += diff * diff;

				nCM++;
			} else {
				nNCM++;
			}
		}

		return Math.sqrt(score) - a * nCM + b * nNCM;
	}

	private void checkScore(double similarity, String floor) {

		if (mostSimilar.size() == 0) {
			mostSimilar.add(new Score(similarity, floor));
			return;
		}

		for (int i = 0; i < mostSimilar.size(); i++) {
			if (mostSimilar.get(i).similarity > similarity) {
				mostSimilar.add(i, new Score(similarity, floor));
				if (mostSimilar.size() > l1) {
					mostSimilar.remove(mostSimilar.size() - 1);
				}
				return;
			}
		}

		if (mostSimilar.size() < l1) {
			mostSimilar.add(new Score(similarity, floor));
		}

	}

	public void proccess(ArrayList<JsonNode> bucket, String floor) {
		double similarity = compare(bucket);
		checkScore(similarity, floor);
	}

	public String getFloor() {
		// Floor -Score
		HashMap<String, Integer> sum_floor_score = new HashMap<String, Integer>();

		for (Score s : mostSimilar) {
			Integer score = 1;
			if (sum_floor_score.containsKey(s.floor)) {
				score = sum_floor_score.get(s.floor) + 1;
			}

			sum_floor_score.put(s.floor, score);
		}

		String max_floor = "0";
		int max_score = 0;

		for (String floor : sum_floor_score.keySet()) {
			int score = sum_floor_score.get(floor);
			if (max_score < score) {
				max_score = score;
				max_floor = floor;
			}
		}

		return max_floor;

	}

	private class Score {

		double similarity;
		String floor;

		Score(double similarity, String floor) {
			this.similarity = similarity;
			this.floor = floor;
		}
	}

	private class Wifi {
		String mac;
		Integer rss;

		Wifi(String mac, Integer rss) {
			this.mac = mac;
			this.rss = rss;
		}
	}

}