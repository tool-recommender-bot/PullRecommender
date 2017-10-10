package com.chbrown13.pull_rec;

import com.jcabi.github.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.json.JsonObject;

/**
 * PullRecommender is the main class for this project and handles interactions with Github repositories.
 */
public class PullRecommender {

	private Repo repo;
	private int recs;
	private Set<String> prs;

	public PullRecommender(Repo repo) {
		this.repo = repo;
		Utils.setProjectName(repo.coordinates().repo());
		Utils.setProjectOwner(repo.coordinates().user());
		prs = new HashSet<String>();
	}

	/**
	 * Gets the number of recommendations made
	 *
	 * @return   Count of recommendations
     */
	public int getRecommendationCount() {
		return this.recs;
	}

	public int getPRCount() {
		return this.prs.size();
	}
	
	/**
	 * Post message recommending ErrorProne to Github on pull request fixing error.
	 *
	 * @param comment   Comment with recommendation
	 * @param pull	 	Pull request to comment on
	 * @param error     Error fixed in pull request
	 */
	private void makeRecommendation(Pull.Smart pull, ErrorProneItem error, String sha, int line) {
		String comment = error.generateComment();
		System.out.println("PR#: "+Integer.toString(pull.number()));
		System.out.println(comment);
		recs += 1;
		prs.add(sha);
	}

	/**
	 * Analyze code of files in pull request and compare to master branch.
	 *
	 * @param pull   Current pull request
	 * @return       Number of recommendations made
	 */
	private void analyze(Pull.Smart pull) {
		System.out.println("Analyzing PR #" + Integer.toString(pull.number()) + "...");
		try {
			String pullHash = pull.json().getJsonObject("head").getString("sha");
			String baseHash = pull.json().getJsonObject("base").getString("sha");
			Iterator<JsonObject> fileit = pull.files().iterator();
			while (fileit.hasNext()) {
				JsonObject file = fileit.next();
				String filename = file.getString("filename");
				System.out.println(filename);
				if (filename.endsWith(".java")) {
					String pullURL = file.getString("raw_url");
					String baseURL = pullURL.replace(pullHash, baseHash);
					String baseTempFile = String.join("_", baseHash, filename);
					String pullTempFile = String.join("_", pullHash, filename);
					Utils.wget(baseURL, baseTempFile);
					Utils.wget(pullURL, pullTempFile);
					String baseLog = ErrorProneItem.analyzeCode(baseTempFile);
					if (baseLog == null || baseLog == "") {
						continue;
					} else {
						String pullLog = ErrorProneItem.analyzeCode(pullTempFile);
						List<ErrorProneItem> baseEP = ErrorProneItem.parseErrorProneOutput(baseLog);
						List<ErrorProneItem> pullEP = ErrorProneItem.parseErrorProneOutput(pullLog);
						for (ErrorProneItem epi: baseEP) {
							int fix = Utils.getFix(baseTempFile, pullTempFile);
							if (!pullEP.contains(epi) && fix > 0) {
								epi.setFilePath(filename);
								System.out.println("Fixed: "+epi.getKey());
								makeRecommendation(pull, epi, pullHash, fix);
							}
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}

	/**
	 * Searches for new pull requests opened for a Github repository every 15 minutes.
	 *
	 * @return   List of new pull requests
	 */
	private ArrayList<Pull.Smart> getPullRequests(int num) {
		System.out.println("Getting new pull requests...");
		ArrayList<Pull.Smart> requests = new ArrayList<Pull.Smart>();
		Map<String, String> params = new HashMap<String, String>();
		params.put("state", "all");
		Iterator<Pull> pullit = this.repo.pulls().iterate(params).iterator();
		while (pullit.hasNext()) {
			Pull.Smart pull = new Pull.Smart(pullit.next());
			try {
				if (pull.number() >= num) {
					requests.add(pull);
					System.out.println("Pull Request #" + Integer.toString(pull.number()) + ": " + pull.title());
				}
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}
		return requests;
	}

	public static void main(String[] args) {
		String[] acct = Utils.getCredentials(".github.creds");
        RtGithub github = new RtGithub(acct[0], acct[1]);
        Repo repo = github.repos().get(new Coordinates.Simple(args[0], args[1]));
		PullRecommender recommender = new PullRecommender(repo);
		ArrayList<Pull.Smart> requests = recommender.getPullRequests(Integer.parseInt(args[2]));
		if (requests != null && !requests.isEmpty()) {
			for (Pull.Smart pull: requests) {
				recommender.analyze(pull);
			}
		} else {
			System.out.println("No pull requests recently opened.");
		}
		System.out.println("{num} recommendations made for {prs} prs.".replace("{num}", Integer.toString(recommender.getRecommendationCount())).replace("{prs}", Integer.toString(recommender.getPRCount())));
	}
}


