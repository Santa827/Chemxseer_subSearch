package edu.psu.chemxseer.structure.supersearch.Experiment;

import java.io.IOException;
import java.text.ParseException;

import de.parmol.graph.Graph;

import edu.psu.chemxseer.structure.preprocess.MyFactory;
import edu.psu.chemxseer.structure.preprocess.RandomChoseDBGraph;
import edu.psu.chemxseer.structure.subsearch.Impl.GraphDatabase_OnDisk;
import edu.psu.chemxseer.structure.subsearch.Interfaces.GraphDatabase;
import edu.psu.chemxseer.structure.supersearch.GraphIntegration.MergeGraph;

public class AIDSLargeExp {
	public static String home = "/data/home/duy113/SupSearchExp/AIDSLargeNew/";
	//public static String home = "/Users/dayuyuan/Documents/workspace/Experiment/";
	
	public static void prePare() throws ParseException, IOException{
		//1. data base
		String dbName = home + "DBFile_Raw";
		GraphDatabase gDB = new GraphDatabase_OnDisk(dbName, MyFactory.getSmilesParser());
		//2. Generate training queries: [normal distribution mean 100, variance 50]
		String trainQuery = home + "trainQuery";
		GaussianGenerator gen = new GaussianGenerator(6, 2);
		float totalCount = 10000;
		int trainSize = (int) (totalCount * 0.8);
		Graph[] gs = new Graph[trainSize];
		for(int i = 0; i< trainSize; i++){
			int count = (int)(gen.getNext());
			if(count < 10){
				i--;
				continue;
			}
			gs[i] = MergeGraph.mergeGraphs(gDB, count);
		}
		RandomChoseDBGraph.saveGDB(gs, MyFactory.getUnCanDFS(), trainQuery);
		//3. Generate test queries
		String testQuery = home + "testQuery";
		int testSize = (int) (totalCount * 0.2);
		gs = new Graph[testSize];
		for(int i = 0; i< testSize; i++){
			int count = (int)(gen.getNext());
			if(count < 10){
				i--;
				continue;
			}
			gs[i] = MergeGraph.mergeGraphs(gDB, count);
		}
		RandomChoseDBGraph.saveGDB(gs, MyFactory.getUnCanDFS(), testQuery);
	}
	
	
	public static void buildIndex() throws ParseException, IOException{
		GraphDatabase trainingDB = new GraphDatabase_OnDisk(home + "DBFile_Raw", MyFactory.getSmilesParser());
		GraphDatabase trainQuery = new GraphDatabase_OnDisk(home + "trainQuery", MyFactory.getUnCanDFS());
		GraphDatabase query = new GraphDatabase_OnDisk(home + "testQuery", MyFactory.getUnCanDFS());		//i > =6
		boolean lucene_im_mem = false;
		double[] minSupts = new double[6];
		minSupts[0] = 0.05; minSupts[1] = 0.03; minSupts[2] = 0.02; minSupts[3]= 0.01;
		minSupts[4]=0.008; minSupts[5] = 0.006;
		int lwIndexCount[] = new int[1];
		lwIndexCount[0] = 590;
		
		for(int i = 3; i< minSupts.length; i++){
			double minSupt = minSupts[i];
			String baseName = home + "MinSup_" + minSupt;
			if(i >= 4){
				System.out.println(baseName + "BuildIndex");
				System.out.println(baseName + "LWIndex");
				LWIndexExp.buildIndex(trainingDB, trainQuery, trainingDB, MyFactory.getUnCanDFS(), baseName, minSupt, lwIndexCount);
				System.gc();
				System.out.println(baseName + "PrefixIndexHi");
				PrefixIndexExp.buildHiIndex(trainingDB, trainingDB, 2, baseName, minSupt);
				System.gc();
				//System.out.println(baseName + "GPTree");
				//GPTreeExp.buildIndex(trainingDB, trainingDB, baseName, minSupt);
				//System.gc();
				
				System.out.println(baseName + "Run The Indexes:");
				System.out.println(baseName + "LWIndex");
				LWIndexExp.runIndex(trainingDB, query, baseName, lucene_im_mem);
				System.gc();
				System.out.println(baseName + "PrefixIndexHi");
				PrefixIndexExp.runHiIndex(trainingDB, query, baseName, lucene_im_mem);
				System.gc();
			}
			else if(i == 3){
				System.out.println(baseName + "GPTree");
				GPTreeExp.runIndex(trainingDB, query, baseName, lucene_im_mem);
				System.gc();
			}
		}
	}
	
	
	public static void memConsumption() throws IOException {
		GraphDatabase trainingDB = new GraphDatabase_OnDisk(home + "DBFile_Raw", MyFactory.getSmilesParser());

		double[] minSupts = new double[6];
		minSupts[0] = 0.05; minSupts[1] = 0.03; minSupts[2] = 0.02; minSupts[3]= 0.01;
		minSupts[4]=0.008; minSupts[5] = 0.006;
		int lwIndexCount[] = new int[1];
		lwIndexCount[0] = 590;
		System.out.println("Posting Memotyr Consumtpion");
		for(int i = 0; i< minSupts.length; i++){
			double minSupt = minSupts[i];
			String baseName = home + "MinSup_" + minSupt;
			System.out.print("LWIndex");
			LWIndexExp.pMemoryConsumption(trainingDB, baseName);
			System.out.print("PrefixHi");
			PrefixIndexExp.pMemoryConsumptionHi(trainingDB, baseName);
			if(i < 4){
				System.out.println("GPTree");
				GPTreeExp.pMemoryConsumption(trainingDB, baseName);
				System.out.println("LWIndexFake");
				LWIndexExp.pMemoryConsumption(trainingDB, home + "MinSup_Fake_" + minSupt);
			}
		}
		System.out.println("Static MemConsumtpion");
		for(int i = 0; i< minSupts.length; i++){
			double minSupt = minSupts[i];
			String baseName = home + "MinSup_" + minSupt;
			System.out.print("LWIndex");
			LWIndexExp.memoryConsumption(trainingDB, baseName, false);
			System.out.print("PrefixHi");
			PrefixIndexExp.memoryConsumptionHi(trainingDB, baseName, false);
			if(i < 4){
				System.out.println("GPTree");
				GPTreeExp.memoryConsumption(trainingDB, baseName, false);
				System.out.println("LWIndexFake");
				LWIndexExp.memoryConsumption(trainingDB, home + "MinSup_Fake_" + minSupt, false);
			}
		}
	}
	
	
	
	public static void main(String[] args){
//		try {
//			prePare();
//		} catch (ParseException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		try {
			buildIndex();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
