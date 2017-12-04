package ReviewerRecommendation.Algorithms.RSVD;

import ReviewerRecommendation.Algorithms.RSVD.RatingMatrix.Iter;

public class BasicSVD implements CFModel{
	
	public int K = 10;
	public int U;
	public int I;
	private double lamda;
	public Double[][] puk;
	public Double[][] qki;

	
	public void setLamda(double lamda) {
		this.lamda = lamda;
	}
	
	public void setK(int K) {
		this.K = K;
	}
	
	@Override
	public void initial(RatingMatrix mat) {
		this.U = mat.getX();
		this.I = mat.getY();
		
		puk = new Double[U][K];
		qki = new Double[K][I];
		
		for (int i = 0; i < U; i++) 
			for (int j = 0; j < K; j++)
				puk[i][j] = Math.random() / 10;
		
		for (int i = 0; i < K; i++) 
			for (int j = 0; j < I; j++)
				qki[i][j] = Math.random() / 10;
	}
	
	public double predictVal(int u, int i) {
		return innerProduct(u, i);
	}
	
	public void updatePara(int u, int i, double trueVal, double step, RatingMatrix mat) {
		double err = error(u, i, trueVal, mat);
//		
//		System.out.println("U : " + u + " I: " + i);
//		System.out.println("error : " + err);
//		System.out.println("step : " + step);
//		System.out.println("trueVal : " + trueVal);
		
		for (int k = 0; k < K; k++) {
			double oldpuk = puk[u][k];
//			double oldqki = qki[k][i];
			
			puk[u][k] = puk[u][k] + 
					step*(err*qki[k][i] - lamda * puk[u][k]);
			qki[k][i] = qki[k][i] + 
					step*(err*oldpuk - lamda * qki[k][i]);
		}
	}
	
	private double error(int u, int i, double trueVal, RatingMatrix mat) {
//		System.out.println("bu[u] : " + bu[u]);
//		System.out.println("bu[i] : " + bu[i]);
		
		double eval = innerProduct(u, i);
		if (eval < mat.getMinRate()) eval = mat.getMinRate();
		if (eval > mat.getMaxRate()) eval = mat.getMaxRate();
		
//		System.out.println("eval : " + eval);
		
		return trueVal - eval;
	}
	
	private double innerProduct(int u, int i) {
		double ret = 0.0;
		for (int j = 0; j < K; j++) {
			ret += puk[u][j] * qki[j][i];
		}
		return ret;
	}
	
	public double calculateCostFunction(RatingMatrix mat) {
		double cost = 0.0;
		
		Iter it = mat.iterateMatrix();
		Double example;
		while (!(example = it.next()).equals(Double.MIN_VALUE)) {
			int u = it.getU();
			int i = it.getI();
			cost += (example - predictVal(u, i)) * (example - predictVal(u, i));
		}
		
//		System.out.println("cost :" + cost);
		double p = 0.0;
		for (int i = 0; i < mat.getX(); i++)
			for (int k = 0; k < K; k ++)
				p += puk[i][k] * puk[i][k];
		
//		System.out.println("p :" + p);
		
		double q = 0.0;
		for (int i = 0; i < mat.getY(); i++)
			for (int k = 0; k < K; k ++)
				q += qki[k][i] * qki[k][i];
		
//		System.out.println("q :" + q);
		
		return 0.5 * cost + 0.5 * lamda * (p + q);
	}
}
