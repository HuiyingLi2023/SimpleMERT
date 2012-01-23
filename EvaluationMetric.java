package mert;

import java.util.LinkedList;

import eval.Stemmer;

public class EvaluationMetric {

	public boolean toBeMinimized;
	String name;
	
	public EvaluationMetric(String metric) {
		// TODO Auto-generated constructor stub
		this.name = metric;
		if(metric.equals("Rouge1"))
		{
			this.toBeMinimized = false;
		}
		else if(metric.equals("Rouge12"))
		{
			this.toBeMinimized = false;
		}
		else
		{
			System.err.println("Unknown metric...");
			System.exit(1);
		}
	}
	
	public Double score(Sentence[] ref, Sentence can)
	{
		String[] reflist = new String[ref.length];
		for(int i =0; i < ref.length; i++)
			reflist[i] = normalize(ref[i]);
		
		Double sc=null;
		if(this.name.equals("Rouge1"))
		{
			return Rouge1(reflist, normalize(can));
		}
		else if(this.name.equals("Rouge12"))
		{
			return Rouge12(reflist, normalize(can));
		}
		return sc;
	}
	
	public String normalize(Sentence s)
	{
		return s.content;
	}
	public Double Rouge12(String[]ref, String can)
	{
		if(ref.length ==1)
		{
			double r1 = rouge1score(can, ref[0]);
			double r2 = rouge2score(can, ref[0]);
			return (r1+r2)/2.0;
		}
		else
			return null;
	}
	public Double Rouge1(String[] ref, String can)
	{
		if(ref.length==1)
		{
			String[] rl = ref[0].toLowerCase().split(" ");
			String[] cl = can.toLowerCase().split(" ");
			boolean[] taken = new boolean[rl.length];
			double count = 0.0;
			for(int i = 0; i < rl.length; i++)
			{
				for(int j = 0; j < cl.length; j++)
				{
					if(isEqual(rl[i],cl[j])&&!taken[i])
					{
						count++;
						taken[i]=true;
					}
				}
			}
			double p = count*1.0/cl.length;
			double r = count*1.0/rl.length;
			if(count!=0)
			return 2*p*r/(p+r);
			else
				return 0.0;
		}
		else
			return null;
	}
	
	public Boolean isEqual(String s1, String s2)
	  {
		  Stemmer st1 = new Stemmer();
		  s1=s1.toLowerCase();
		  s2=s2.toLowerCase();
		  for(int i = 0; i < s1.length()&&Character.isLetter(s1.charAt(i)); i++)
		  {
			  st1.add(s1.charAt(i));
		  }
		  st1.stem();
		  Stemmer st2 = new Stemmer();
		  for(int i = 0; i < s2.length()&&Character.isLetter(s2.charAt(i)); i++)
			  st2.add(s2.charAt(i));
		  st2.stem();
		  
		  if(s1.toString().equals(s2.toString()))
			  return true;
		  else
			  return false;
			  
	  }
	public boolean isBetter(Double newScore, Double oldScore)
	{
		if(toBeMinimized && newScore<oldScore)
			return true;
		else if(!toBeMinimized && newScore>oldScore)
			return true;
		else 
			return false;
	}
	public double rouge1score(String can, String ref) {
		String[] lcan = can.split(" ");
		String[] lref = ref.split(" ");
		boolean[] taken = new boolean[lref.length];
		int count = 0;
		for (int i = 0; i < taken.length; i++)
			taken[i] = false;
		for (int i = 0; i < lcan.length; i++) {
			for (int j = 0; j < lref.length; j++) {
				if (isEqual(lcan[i], lref[j]) && taken[j] == false) {
					taken[j] = true;
					count++;
				}
			}
		}
		double p = 1.0 * count / lcan.length;
		double r = 1.0 * count / lref.length;
		if (count == 0)
			return 0.0;
		else
			return 2 * p * r / (p + r);

	}

	public static double rouge2score(String can, String ref)
	{
		String[] lcan = can.split(" ");
		String[] lref = ref.split(" ");
		//stem all the words
		for(int i = 0; i < lcan.length; i++)
		{
			Stemmer st = new Stemmer();
			String s = lcan[i].toLowerCase();
			for (int j = 0; j < s.length() && Character.isLetter(s.charAt(j)); j++) {
				st.add(s.charAt(j));
			}
			st.stem();
			lcan[i]=st.toString();
		}
		for(int i = 0; i < lref.length; i++)
		{
			Stemmer st = new Stemmer();
			String s = lref[i].toLowerCase();
			for (int j = 0; j < s.length() && Character.isLetter(s.charAt(j)); j++) {
				st.add(s.charAt(j));
			}
			st.stem();
			lref[i]=st.toString();
		}
		LinkedList<String> canbi = new LinkedList<String>();
		LinkedList<String> refbi = new LinkedList<String>();
		for(int i = 0; i < lcan.length-1; i++)
		{
			String s1 = lcan[i];
			String s2 = lcan[i+1];
			if(s1.length()>0&&s2.length()>0)
				canbi.add(s1+" "+s2);
		}
		for(int i = 0; i < lref.length-1; i++)
		{
			String s1 = lref[i];
			String s2 = lref[i+1];
			if(s1.length()>0&&s2.length()>0)
				refbi.add(s1+" "+s2);
		}
		int count=0;
		boolean[] taken = new boolean[refbi.size()];
		for(int i = 0; i < taken.length; i++)
			taken[i] = false;
		for(int i = 0; i < canbi.size(); i++)
		{
			for(int j = 0; j < refbi.size(); j++)
			{
				if(!taken[j]&&canbi.get(i).equals(refbi.get(j)))
				{
					taken[j]=true;
					count++;
				}
			}
		}
		double p = 1.0*count/canbi.size();
		double r = 1.0*count/refbi.size();
		if(count==0)
			return 0;
		else
			return 2*p*r/(p+r);
	}
	
	public static void main(String[] args)
	{
		String[] ref = {"This is a test stepped away sleeping"};
		String can = "this tested to go sleep";
		EvaluationMetric em = new EvaluationMetric("Rouge12");
		Sentence[] refl = new Sentence[1];
		refl[0] = new Sentence();
		refl[0].content = ref[0];
		Sentence cansen = new Sentence();
		cansen.content = can;
		System.out.println(em.score(refl, cansen));
	}
}
