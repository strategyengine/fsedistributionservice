package com.strategyengine.xrpl.fsedistributionservice.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class AnalysisLogParser {

	public Map<String, List<String>> getRelationshipsFromLog() throws Exception {

		Set<String> addressLines;
		try (InputStream inputStream = getClass().getResourceAsStream("/fse_analysis_output.txt");
				BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
			List<String> lines = reader.lines().collect(Collectors.toList());
			addressLines = lines.stream()
					.filter(s -> s != null && (s.contains("count:") || s.contains("Skipping account")))
					.collect(Collectors.toSet());
		}

		List<Rel> relCounts = addressLines.stream().filter(s -> s.contains("count:")).map(s -> convertCount(s))
				.collect(Collectors.toList());
		relCounts.addAll(addressLines.stream().filter(s -> s.contains("Skipping account")).map(s -> convertSkipping(s))
				.collect(Collectors.toList()));

		Map<String, List<String>> m = new HashMap<String, List<String>>();
		for (Rel r : relCounts) {

			if (r.parent == null || r.parent.equals("null")) {
				continue;
			}
			List<String> children = m.get(r.parent);
			if (children == null) {
				children = new ArrayList<String>();
			}
			children.add(r.child);
			m.put(r.parent, children);
		}
		return m;
	}

	private Rel convertSkipping(String s) {

		int childStart = s.indexOf("account=") + 8;
		int parentStart = s.indexOf("parent=") + 7;

		int childEnd = s.indexOf(",", childStart);
		int parentEnd = s.indexOf(",", parentStart);

		String parent = s.substring(parentStart, parentEnd);
		String child = s.substring(childStart, childEnd);

		Rel r = new Rel();
		r.child = child;
		r.parent = parent;
		return r;

	}

	private Rel convertCount(String s) {

		try {
			int childStart = s.indexOf("activated address") + 19;
			int parentStart = s.indexOf("Parent:") + 8;
			int parentEnd = s.indexOf(" ", parentStart);

			String parent = s.substring(parentStart, parentEnd);
			String child = s.substring(childStart);

			Rel r = new Rel();
			r.child = child;
			r.parent = parent;
			return r;
		} catch (Exception e) {
			System.out.println(s);
			throw e;
		}

	}
}

class Rel {
	String parent;
	String child;
}