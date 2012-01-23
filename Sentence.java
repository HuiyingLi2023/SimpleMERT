package mert;

public class Sentence {
	int ClusterID;
	int ID;
	String content;

	Double[] featVal = null;
	
	public String toString()
	{
		return this.ClusterID+"-"+this.ID+":"+this.content;
	}
	public Double getOverallDecoderScore(Double[] lambdas)
	{
		if(featVal.length!= lambdas.length)
		{
			System.out.println("Feature length!= lambda length.");
			System.exit(1);
		}
		if(featVal!=null)
		{
			double sum = 0.0;
			for(int i = 0; i < this.featVal.length; i++)
			{
				sum+=featVal[i]*lambdas[i];
			}
			return sum;
		}
		else
			return null;
	}
}
