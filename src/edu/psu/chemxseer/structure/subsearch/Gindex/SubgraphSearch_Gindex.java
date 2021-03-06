package edu.psu.chemxseer.structure.subsearch.Gindex;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;


import de.parmol.graph.Graph;
import edu.psu.chemxseer.structure.subsearch.Impl.VerifierISO;
import edu.psu.chemxseer.structure.subsearch.Interfaces.GraphFetcher;
import edu.psu.chemxseer.structure.subsearch.Interfaces.GraphResult;
import edu.psu.chemxseer.structure.subsearch.Interfaces.IndexSearcher;
import edu.psu.chemxseer.structure.subsearch.Interfaces.PostingFetcher;
import edu.psu.chemxseer.structure.subsearch.Interfaces.SubgraphSearch;
import edu.psu.chemxseer.structure.supersearch.Impl.PostingBuilderMem;

public class SubgraphSearch_Gindex implements SubgraphSearch{
	public IndexSearcher indexSearcher;
	public PostingFetcher postingFetcher;
	public VerifierISO verifier;
	
	public SubgraphSearch_Gindex(IndexSearcher indexSearcher, PostingFetcher postingFetcher, 
			VerifierISO verifier){
		this.indexSearcher = indexSearcher;
		this.postingFetcher = postingFetcher;
		this.verifier = verifier;
		
	}
	
	
	public  List<GraphResult> getAnswer(Graph query, long[] TimeComponent, int[] Number)
			throws IOException, ParseException {
		TimeComponent[0] = TimeComponent[1] =TimeComponent[2] =TimeComponent[3]= 0;
		Number[0] = Number [1] = 0;

		 List<GraphResult> answer = null;
		int[] temp = new int[1];
		answer = this.hitAndReturn(query,temp, TimeComponent);

		if(answer!=null){
			Number[0] = 0; // No verification is needed
		}
		else{
			GraphFetcher candidateFetcher;
			candidateFetcher = this.candidateByFeatureJoin(query, TimeComponent);
			Number[0] = candidateFetcher.size();
			answer = this.verifier.verify(query, candidateFetcher, true, TimeComponent);
		}
		Number[1] = answer.size();
		return answer;
	}
	
	private  List<GraphResult> hitAndReturn(Graph query, int[] hitIndex, long[] TimeComponent){
		
		boolean[] exactMatch = new boolean[1];
		exactMatch[0] = false;
		hitIndex[0] = indexSearcher.designedSubgraph(query, exactMatch,TimeComponent);
		if(hitIndex[0] == -1)
			return null;
		else if(exactMatch[0]){
			List<GraphResult> result = null;
			GraphFetcher gf = this.postingFetcher.getPosting(hitIndex[0], TimeComponent);
			result = gf.getAllGraphs(TimeComponent);
			return result;
		}
		else return null;
	}
	
	
	public  GraphFetcher candidateByFeatureJoin(Graph query, long[] TimeComponent){
		List<Integer> features = indexSearcher.maxSubgraphs(query, TimeComponent);
		if(features == null || features.size() == 0)
			return null;
		else return postingFetcher.getJoin(features, TimeComponent);
	}
	/*********The Flowing Will be Replace Soon *****************************/
	public static String getLuceneName() {
		return "lucene/";
	}


	public static String getIndexName() {
		// TODO Auto-generated method stub
		return "index";
	}


	@Override
	public PostingBuilderMem getInMemPosting() {
		// TODO Not Implemented
		return null;
	}
	
//	public void test(Graph g, int gID){
//		long[] time = new long[4];
//		int[] subs = this.indexSearcher.subgraphs(g, time);
//		for(int i = 0; i< subs.length;i++){
//			List<GraphResult> postings = this.postingFetcher.getPosting(subs[i], time);
//			boolean contain = false;
//			for(GraphResult onePos : postings){
//				if(onePos.getID() == gID){
//					contain = true;
//					break;
//				}
//			}
//			if(contain==false){
//				System.out.println("Ill build " + gID  + "," + subs.length);
//			}
//		}
//	}

}
