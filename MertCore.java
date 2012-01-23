package mert;

import java.util.*;
import java.io.*;

public class MertCore {

	String decoderOuput;
	String decoderCmd;
	String parameters;
	String refPath;
	String decoderCfg;

	public int canPerCluster;
	public int refPerCluster;
	public int NCluster;
	public int NFeat;
	public double Ninterval;
	public int Nexp = 5;
	public long seed = 1234567;
	public String metric;

	// use linkedlist because the cumulative candidate set may increase each
	// iteration
	public LinkedList<LinkedList<Sentence>> canSet;
	public Sentence[][] refSet;
	public double[][] paraRanges;
	public Double[] currentLambdas;
	public String[] paraNames;
	public boolean[] optimizable;

	public EvaluationMetric em;

	public Double curEvalScore;

	public void parseArgs(String configPath) throws IOException {
		String[] args = customReader.Reader
				.retriveContentFromFileByLine(new File(configPath));
		this.decoderOuput = args[0].substring(0, args[0].indexOf("#")).trim();
		this.decoderCmd = args[1].substring(0, args[1].indexOf("#")).trim();
		this.parameters = args[2].substring(0, args[2].indexOf("#")).trim();
		this.refPerCluster = Integer.parseInt(args[3].substring(0,
				args[3].indexOf("#")).trim());
		this.canPerCluster = Integer.parseInt(args[4].substring(0,
				args[4].indexOf("#")).trim());
		this.NCluster = Integer.parseInt(args[5].substring(0,
				args[5].indexOf("#")).trim());
		this.Ninterval = Integer.parseInt(args[6].substring(0,
				args[6].indexOf("#")).trim());
		this.metric = args[7].substring(0, args[7].indexOf("#")).trim();
		this.refPath = args[8].substring(0, args[8].indexOf("#")).trim();
		this.decoderCfg = args[9].substring(0, args[9].indexOf("#")).trim();

	}

	public void processParameters() {
		String[] l = customReader.Reader.retriveContentFromFileByLine(new File(
				this.parameters));
		this.NFeat = l.length;
		this.paraNames = new String[this.NFeat];
		this.currentLambdas = new Double[this.NFeat];
		this.paraRanges = new double[this.NFeat][2];
		this.optimizable = new boolean[this.NFeat];

		for (int i = 0; i < l.length; i++) {
			this.paraNames[i] = l[i].split("\\s+")[0];
			this.currentLambdas[i] = Double.parseDouble(l[i].split("\\s+")[1]);
			this.paraRanges[i][0] = Double.parseDouble(l[i].split("\\s+")[2]);
			this.paraRanges[i][1] = Double.parseDouble(l[i].split("\\s+")[3]);
			String s = l[i].split("\\s+")[4];
			if (s.equals("opt"))
				this.optimizable[i] = true;
			else if (s.equals("fix"))
				this.optimizable[i] = false;
			else {
				System.err
						.println("Optimizable? only opt and fix are recognized...");
				System.exit(1);
			}
		}
	}

	public void initialize() {
		em = new EvaluationMetric(this.metric);
		if (em.toBeMinimized)
			curEvalScore = Double.POSITIVE_INFINITY;
		else
			curEvalScore = Double.NEGATIVE_INFINITY;

		processParameters();// here default values are read into the
		// currentLambdas array
		loadRefSet();
		readDecoderOuput();

	}

	public void loadRefSet() {
		this.refSet = new Sentence[this.NCluster][this.refPerCluster];
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(this.refPath));
			String line = null;
			for (int i = 0; i < NCluster; i++) {
				for (int j = 0; j < refPerCluster; j++) {
					line = br.readLine();
					Sentence s = new Sentence();
					s.ClusterID = i;
					s.ID = j;
					s.content = line;
					this.refSet[i][j] = s;
				}
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			System.err.println("Cannot find reference file...");
			e.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.err.println("Reference file not well formatted...");
			e.printStackTrace();
		}

	}

	public boolean readDecoderOuput() {
		boolean addNew = false;
		File f = new File(this.decoderOuput);
		if (f.exists()) {
			LinkedList<Hashtable<String, Boolean>> existingCan = new LinkedList<Hashtable<String, Boolean>>();
			if (this.canSet == null)
				this.canSet = new LinkedList<LinkedList<Sentence>>();
			else {// load existing candidates into hashtable
				for (Iterator iterator = this.canSet.iterator(); iterator
						.hasNext();) {
					LinkedList<Sentence> sentence = (LinkedList<Sentence>) iterator
							.next();
					Hashtable<String, Boolean> ht = new Hashtable<String, Boolean>();
					for (Iterator iterator2 = sentence.iterator(); iterator2
							.hasNext();) {
						Sentence sentence2 = (Sentence) iterator2.next();
						ht.put(sentence2.content, true);
					}
					existingCan.add(ht);
				}
			}
			try {
				BufferedReader br = new BufferedReader(new FileReader(f));
				String line = null;
				for (int i = 0; i < NCluster; i++) {
					LinkedList<Sentence> c = null;
					if (this.canSet.size() > i)
						c = this.canSet.get(i);
					else
						c = new LinkedList<Sentence>();
					for (int j = 0; j < canPerCluster; j++) {
						line = br.readLine();
						String[] l = line.split("\\|\\|\\|");
						if (existingCan.size() <= i)
							existingCan.add(new Hashtable<String, Boolean>());
						if (!existingCan.get(i).containsKey(l[1])) {
							Sentence s = new Sentence();
							s.ClusterID = Integer.parseInt(l[0]);
							s.ID = j;
							s.content = l[1];
							String[] fl = l[2].split("\\s+");
							if (fl.length != NFeat) {
								System.err.println("Decoder not outputting "
										+ NFeat + " features...");
								System.exit(1);
							}
							s.featVal = new Double[NFeat];
							for (int k = 0; k < NFeat; k++)
								s.featVal[k] = Double.parseDouble(fl[k]);
							c.add(s);
							addNew = true;
						}
					}
					if (this.canSet.size() <= i)
						this.canSet.add(c);
				}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				System.err.println("Cannot find decoder output file...");
				e.printStackTrace();
				System.exit(1);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.err.println("Decoder ouput file not well formatted...");
				e.printStackTrace();
				System.exit(1);
			}

		}
		return addNew;
	}

	public void MERT() {
		if (this.canSet == null)// this is the fist iteration, should run the
		// decoder first
		{
			System.out.println("Initially, decoder will be run with: ");
			for (int i = 0; i < this.NFeat; i++)
				System.out.print(this.paraNames[i] + ":"
						+ this.currentLambdas[i] + " ");
			System.out.println();
			runDecoder();
			readDecoderOuput();
		}
		int it = 0;
		while (true) {
			System.out.println("Begin iteration " + (++it));
			Double[] result = singleIteration(this.currentLambdas);
			// if not equal to current lambdas
			boolean isChanged = false;
			for (int i = 0; i < result.length; i++) {
				if (result[i] != this.currentLambdas[i]) {
					isChanged = true;
					break;
				}
			}
			if (!isChanged)
				break;
			//Added on 2012.1.12, following 2 lines
			else
				this.currentLambdas = result;
			createDecoderConfigFile(this.currentLambdas);

			runDecoder();
			if (!readDecoderOuput())// no new candidate added, break
				break;
		}
	}

	public Double[] singleIteration(Double[] initialLambdas) {
		Double[] initialEvalScoreArr = new Double[this.NCluster];
		// now, for each cluster, find out under the specific lambdas, the best
		// evaluation score the cumulative candidate set can reach
		Double es = 0.0;
		for (int i = 0; i < initialEvalScoreArr.length; i++) {
			initialEvalScoreArr[i] = findBestEvalScoreOnSingleCumulativeCanSet(
					this.canSet.get(i), this.currentLambdas);
			es += initialEvalScoreArr[i];
		}
		// if (em.isBetter(es, this.curEvalScore))
		// this.curEvalScore = es;

		// now, begin iteration
		Double[] tempLambdas = new Double[NFeat];
		for (int j = 0; j < NFeat; j++)// copy the value of initial
		// lambdas...
		{
			tempLambdas[j] = initialLambdas[j];
		}

		Double initialEvalScore = es / NCluster;
		Double[] finalLambdas = null;
		// Double[][] expLambdas = generateExploitLambdas();
		Double[] res = { -1.0, 0.0, initialEvalScore };
		while (true) {
			res = singleLoop(tempLambdas, res[2]);
			if (res[0] == -1) {
				if (em.isBetter(res[2], this.curEvalScore)) {
					finalLambdas = tempLambdas;// tempLambdas is modified in
					// 'singleLoop'
					this.curEvalScore = res[2];
				}
				else//added on 2012.1.12
				{
					finalLambdas = this.currentLambdas;
				}
				break;
			}
		}
		// What does the following code mean? currentLambdas does not store next
		// run's lambda here
		// System.out.println("Decoder will be run with: ");
		// for(int i = 0; i < this.NFeat; i++)
		// System.out.print(this.currentLambdas[i]+" ");
		System.out.println("Decoder will be run with: ");
		for (int i = 0; i < this.NFeat; i++)
			System.out.print(this.paraNames[i] + ":" + finalLambdas[i] + " ");
		System.out.println(this.em.name + " value:" + res[2]);
		return finalLambdas;
	}

	/**
	 * 
	 * @param initialLambdas
	 * @return [0]the index of the lambda should be changed, -1 indicates no
	 *         further improvement. [1]the value to change to [2]the eval score
	 *         of the new lambdas
	 */
	public Double[] singleLoop(Double[] initialLambdas, Double initialEvalScore) {

		int whichLambda = -1;
		double whatValue = 0.0;

		// for every dimension that is optimizable, change the value of lambda; for each lambda,
		// find out the best possible eval score
		for (int i = 0; i < NFeat; i++) {
			// we do not apply Och's efficient search. search by interval is
			// performed.
			// this will not lead to global optimum, but might be enough for my
			// current application
			if (this.optimizable[i]) {
				double low = this.paraRanges[i][0];
				double up = this.paraRanges[i][1];
				Double[] tempLambdas = new Double[NFeat];
				for (int j = 0; j < NFeat; j++)// copy the value of initial
				// lambdas...
				{
					tempLambdas[j] = initialLambdas[j];
				}
				// ...and change the ith lambda, find possible eval scores over
				// all
				// clusters
				for (int j = 0; j < Ninterval; j++) {
					Double tempVal = low + j * (up - low) / Ninterval;
					tempLambdas[i] = tempVal;
					Double tempEvalScore = 0.0;
					// calc eval score on this gorup of lambdas over all
					// clusters
					for (int k = 0; k < NCluster; k++) {
						tempEvalScore += findBestEvalScoreOnSingleCumulativeCanSet(
								this.canSet.get(k), tempLambdas);
					}
					tempEvalScore /= NCluster;
					if (em.isBetter(tempEvalScore, initialEvalScore)) {
						initialEvalScore = tempEvalScore;
						whichLambda = i;
						whatValue = tempVal;
					}
				}
			}
		}

		Double[] result = new Double[3];
		result[0] = whichLambda * 1.0;
		result[1] = whatValue;
		result[2] = initialEvalScore;
		if (whichLambda != -1)// some lambda improves!
		{
			initialLambdas[whichLambda] = whatValue;
		}
		return result;
	}

	public Double findBestEvalScoreOnSingleCumulativeCanSet(
			LinkedList<Sentence> cumuCanSet, Double[] lambdas) {
		// first, find out the sentence with best decoder score. Attention, the
		// higher, the better is assumne!!
		Double decScr = Double.NEGATIVE_INFINITY;
		Sentence bestSen = null;
		for (Iterator iterator = cumuCanSet.iterator(); iterator.hasNext();) {
			Sentence sentence = (Sentence) iterator.next();
			Double s = sentence.getOverallDecoderScore(lambdas);
			if (s > decScr) {
				decScr = s;
				bestSen = sentence;
			}
		}
		// compute the eval score for the sentence with best decoder score
		Sentence[] ref = this.refSet[bestSen.ClusterID];
		return this.em.score(ref, bestSen);
	}

	public void runDecoder() {
		try {
			System.out.println("Running the decoder...");
			Runtime rt = Runtime.getRuntime();
			Process p = rt.exec(this.decoderCmd);

			StreamGobbler errorGobbler = new StreamGobbler(p.getErrorStream(),
					1);
			StreamGobbler outputGobbler = new StreamGobbler(p.getInputStream(),
					1);

			errorGobbler.start();
			outputGobbler.start();

			int decStatus = p.waitFor();
			System.out.println("Finish decoding");
			if (decStatus != 0) {
				System.out.println("Call to decoder returned " + decStatus
						+ "; was expecting 0");
				BufferedInputStream in = new BufferedInputStream(p
						.getErrorStream());
				BufferedReader br = new BufferedReader(
						new InputStreamReader(in));
				String msg = null;
				while ((msg = br.readLine()) != null)
					System.out.println(msg);

				System.exit(30);
			}
		} catch (IOException e) {
			System.err.println("IOException in MertCore.run_decoder(int): "
					+ e.getMessage());
			System.exit(99902);
		} catch (InterruptedException e) {
			System.err
					.println("InterruptedException in MertCore.run_decoder(int): "
							+ e.getMessage());
			System.exit(99903);
		}

	}

	public void createDecoderConfigFile(Double[] lambdas) {
		try {
			LinkedList<String> tw = new LinkedList<String>();
			BufferedReader br = new BufferedReader(new FileReader(
					this.decoderCfg));
			String line = null;
			boolean isPara = false;
			int i = 0;
			while ((line = br.readLine()) != null) {
				if (line.startsWith("#")) {
					tw.add(line);
					isPara = true;
					continue;
				}
				if (isPara) {
					tw.add(line.split(" ")[0] + " " + lambdas[i]);
					i++;
				} else {
					tw.add(line);
				}
			}
			br.close();
			customReader.Reader.writeContentToFileByLine(tw, new File(
					this.decoderCfg));

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public Double[][] generateExploitLambdas() {
		Double[][] expLambdas = new Double[Nexp][NFeat];
		Random r = new Random();
		r.setSeed(this.seed);
		for (int i = 0; i < Nexp; i++) {
			for (int j = 0; j < NFeat; j++) {
				Double d = r.nextDouble();
				expLambdas[i][j] = d
						% (this.paraRanges[j][1] - this.paraRanges[j][0])
						+ this.paraRanges[j][0];

			}
		}
		return expLambdas;

	}

	public void createDecoderConfigFile(Double[] lambdas, String output) {
		try {
			LinkedList<String> tw = new LinkedList<String>();
			BufferedReader br = new BufferedReader(new FileReader(
					this.decoderCfg));
			String line = null;
			boolean isPara = false;
			int i = 0;
			while ((line = br.readLine()) != null) {
				if (line.startsWith("#")) {
					tw.add(line);
					isPara = true;
					continue;
				}
				if (isPara) {
					tw.add(line.split(" ")[0] + " " + lambdas[i]);
					i++;
				} else {
					tw.add(line);
				}
			}
			br.close();
			customReader.Reader.writeContentToFileByLine(tw, new File(
					this.decoderCfg.substring(0, this.decoderCfg
							.lastIndexOf("/") + 1)
							+ output));

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		MertCore trainer = new MertCore();
		trainer.parseArgs(args[0]);
		// trainer.parseArgs("/home/amelielee/TitleGeneration/myMERT/mert.config");
		trainer.initialize();
		trainer.MERT();
		Double[] finalLambda = trainer.currentLambdas;
		System.out.println("Final Lambdas:");
		for (int i = 0; i < trainer.NFeat; i++) {
			System.out.println(finalLambda[i] + " ");
		}
		Date d = new Date();
		String outfile = "";
		outfile += "decoder.config." + d.getMonth() + "." + d.getDay() + "."
				+ d.getHours() + "." + d.getMinutes() + "." + d.getSeconds();
		trainer.createDecoderConfigFile(finalLambda, outfile);
	}

}

// based on:
// http://www.javaworld.com/javaworld/jw-12-2000/jw-1229-traps.html?page=4
class StreamGobbler extends Thread {
	InputStream istream;
	boolean verbose;

	StreamGobbler(InputStream is, int p) {
		istream = is;
		verbose = (p != 0);
	}

	public void run() {
		try {
			InputStreamReader isreader = new InputStreamReader(istream);
			BufferedReader br = new BufferedReader(isreader);
			String line = null;
			while ((line = br.readLine()) != null) {
				if (verbose)
					System.out.println(line);
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
}
