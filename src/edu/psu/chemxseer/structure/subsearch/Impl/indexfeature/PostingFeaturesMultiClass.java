package edu.psu.chemxseer.structure.subsearch.Impl.indexfeature;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.List;

import edu.psu.chemxseer.structure.setcover.featureGenerator.IFeatureSetConverter;
import edu.psu.chemxseer.structure.setcover.sets.CoverSet_FeatureWrapper2;
import edu.psu.chemxseer.structure.util.IntersectionSet;


public class PostingFeaturesMultiClass {
	private FeaturePosting[] postingFetchers;
	private NoPostingFeatures<OneFeatureMultiClass> features;
	private int[] classGraphCount;
	
	/**
	 * Given the Features (MultiClassFetures) & The Posting FileName
	 * Construct a PostingMultiClassFeatures class
	 * @param postingFiles
	 * @param features
	 */
	public PostingFeaturesMultiClass(String[] postingFiles, 
			NoPostingFeatures<OneFeatureMultiClass> features, int[]classGraphCount){
		this.features = features;
		this.postingFetchers = new FeaturePosting[postingFiles.length];
		for(int i = 0; i< postingFetchers.length; i++)
			postingFetchers[i] = new FeaturePosting(postingFiles[i]);
		this.classGraphCount = classGraphCount;
	}
	
	private PostingFeaturesMultiClass(FeaturePosting[] postingFetchers,
			NoPostingFeatures<OneFeatureMultiClass> features, int[]classGraphCount){
		this.features = features;
		this.postingFetchers = postingFetchers;
		this.classGraphCount = classGraphCount;
	}
	/**
	 * Given the Feature, return the featurePostings
	 * @param feature
	 * @return
	 */
	public int[][] getPosting(OneFeatureMultiClass feature) {
		long[] postingShift = feature.getAllPostingShift();
		int[][] postings = new int[postingShift.length][];
		for(int i = 0; i< postings.length; i++){
			postings[i] = postingFetchers[i].getPosting(postingShift[i]);
		}
		return postings;
	}
	
	public int[] getUnEqualPosting(OneFeatureMultiClass feature, int classID){
		long[] postingShift = feature.getAllPostingShift();
		int index= classID * 2;
		int[] postings = postingFetchers[index].getPosting(postingShift[index]);
		return postings;
	}
	
	public int[] getEqualPosting(OneFeatureMultiClass feature, int classID){
		long[] postingShift = feature.getAllPostingShift();
		int index= classID * 2+1;
		int[] postings = postingFetchers[index].getPosting(postingShift[index]);
		return postings;
	}
	
	public int[] getFullPosting(OneFeatureMultiClass feature, int classID){
		int[] unequal = this.getUnEqualPosting(feature, classID);
		int[] equal = this.getEqualPosting(feature, classID);
		if(equal.length == 0)
			return unequal;
		else{
			IntersectionSet set = new IntersectionSet();
			set.addAll(equal);
			set.addAll(unequal);
			return set.getItems();
		}
	}
	/**
	 * Return the feature postings of the "ith" feature
	 * @param featureID
	 * @return
	 */
	public int[][] getPosting(Integer featureID) {
		return this.getPosting(features.getFeature(featureID));
	}
	public int[] getUnEqualPosting(int fID, int classID) {
		return this.getUnEqualPosting(features.getFeature(fID), classID);
	}
	
	public int[] getEqualPosting(int fID, int classID){
		return this.getEqualPosting(features.getFeature(fID), classID);
	}
	
	public int[] getFullPosting(int fID, int classID){
		return this.getFullPosting(features.getFeature(fID), classID);
	}

	public PostingFeaturesMultiClass getSelectedFeatures(
			String newFeatureFile, String[] newPostingFiles, boolean reserveID){
		//1. Build the new selected features (In memory)
		List<OneFeatureMultiClass> selectedFeatures = this.features.getSelectedFeatures();
		//2. 
		try {
			return this.saveFeatures(newFeatureFile, newPostingFiles, reserveID, selectedFeatures);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public PostingFeaturesMultiClass getUnSelectedFeatures(
			String newFeatureFile, String[] newPostingFiles, boolean reserveID)
			throws IOException {
		//1. Build the new selected features (In memory)
		List<OneFeatureMultiClass> selectedFeatures = this.features.getUnSelectedFeatures();
		//2. 
		try {
			return this.saveFeatures(newFeatureFile, newPostingFiles, reserveID, selectedFeatures);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	
	private PostingFeaturesMultiClass saveFeatures(String newFeatureFile, 
			String[] newPostingFiles, boolean reserveID, List<OneFeatureMultiClass> selectedFeatures) 
	throws IOException{
		//2. Record the Postings
		if(newPostingFiles!=null){
			FileChannel[] postingChannels = new FileChannel[newPostingFiles.length];
			for(int i = 0; i< newPostingFiles.length; i++){
				if(newPostingFiles[i]!=null){
					postingChannels[i] = new FileOutputStream(newPostingFiles[i]).getChannel();
				}
			}
			int index = 0;
			for(OneFeatureMultiClass oneFeature : selectedFeatures){
				int fID = index;
				if(reserveID)
					fID = oneFeature.getFeatureId();
				long[] shift = oneFeature.getAllPostingShift();
				long[] newShift = new long[shift.length];
				for(int w = 0; w < shift.length; w++)
					if(postingChannels[w]!=null)
					  newShift[w] = this.postingFetchers[w].savePostings(
							  postingChannels[w], shift[w], fID);
					else newShift[w] = -1;
				oneFeature.setPostingShifts(newShift);
				index++;
			}
			// finalize
			for(int i = 0; i< postingChannels.length; i++){
				if(postingChannels[i]!=null)
					postingChannels[i].close();
			}
		}
		//3. Save the Features
		NoPostingFeatures<OneFeatureMultiClass> newFeatures = 
			new NoPostingFeatures<OneFeatureMultiClass>(newFeatureFile, selectedFeatures, reserveID);
		//4. Return 
		if(newPostingFiles !=null)
			return new PostingFeaturesMultiClass(newPostingFiles, newFeatures, this.classGraphCount);
		else return new PostingFeaturesMultiClass(this.postingFetchers, newFeatures, this.classGraphCount);
	}
	public int[] getClassGraphsCount() {
		return this.classGraphCount;
	}
	public NoPostingFeatures <OneFeatureMultiClass> getFeatures() {
		return this.features;
	}
	public CoverSet_FeatureWrapper2[] toWrapper(IFeatureSetConverter converter) {
		CoverSet_FeatureWrapper2[] results = new CoverSet_FeatureWrapper2[features.getfeatureNum()];
		for(int i = 0; i< features.getfeatureNum(); i++){
			results[i] = new CoverSet_FeatureWrapper2(this, i,converter.getItemCount(results[i]));
		}
		return results;
	}

	
	
}
