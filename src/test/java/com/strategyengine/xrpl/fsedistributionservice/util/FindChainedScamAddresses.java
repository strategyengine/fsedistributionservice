package com.strategyengine.xrpl.fsedistributionservice.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;


public class FindChainedScamAddresses {

	private AnalysisLogParser parser = new AnalysisLogParser();

	Set<String> blacklist = new HashSet<>();

	Map<String, Integer> chainSize = new HashMap<>();
	
	ObjectMapper mapper = new ObjectMapper();


//	@Test
	public void findChainScammers() throws Exception {

		Map<String, List<String>> rel = parser.getRelationshipsFromLog();

		rel.keySet().stream().forEach(a -> getChain(a, rel));

		System.out.println("LONG_CHAIN_SCAMMERS: " + mapper.writeValueAsString(blacklist));

	}

	private Chain getChain(String a, Map<String, List<String>> rel) {

		Chain chain = new Chain();
		chain.parent = a;
		List<String> path = new ArrayList<String>();
		addToPath(a, rel, path, 1);

		chain.pathToParent = path;
		if (chain.pathToParent.size() > 40) {
//			System.out.println(chain.parent + " " + chain.pathToParent);
			blacklist.add(chain.parent);
			blacklist.addAll(chain.pathToParent);
		}
		int addSize = 0;
		if (chain.pathToParent != null && !chain.pathToParent.isEmpty()) {
			String lastParent = chain.pathToParent.get(chain.pathToParent.size()-1);
			addSize = chainSize.get(lastParent) != null ? chainSize.get(lastParent) : 0;
		}
		chainSize.put(a, chain.pathToParent.size() + addSize);
		return chain;
	}

	private void addToPath(String a, Map<String, List<String>> rel, List<String> path, int depth) {
		Optional<String> parent = rel.keySet().stream().filter(k -> rel.get(k).contains(a)).findAny();

		depth++;
		if (depth > 6000) {
			System.out.println("MASSIVE DEPTH " + path);
			return;
		}

		if (parent.isEmpty() || parent.get() == null) {
			return;
		}

		if (chainSize.get(parent.get()) != null) {
			return;
		}

		path.add(parent.get());
		// System.out.println("Depth " + depth);
		addToPath(parent.get(), rel, path, depth);
	}

}

class Chain {

	String parent;
	List<String> pathToParent;
	int parentSize = 0;
}