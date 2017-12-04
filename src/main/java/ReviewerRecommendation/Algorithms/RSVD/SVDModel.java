package ReviewerRecommendation.Algorithms.RSVD;

import ReviewerRecommendation.Algorithms.RSVD.RatingMatrix.Iter;

public class SVDModel implements CFModel{
	
	public int K = 10;
	public int U;
	public int I;
	private double lamda;
	public Double[][] puk;
	public Double[][] qki;
	public Double[] bu;
	public Double[] bi;
	public double averRate;
	
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
		bu = new Double[U];
		bi = new Double[I];
		
		for (int i = 0; i < U; i++) 
			for (int j = 0; j < K; j++)
				puk[i][j] = Math.random() / 10;
		
		for (int i = 0; i < K; i++) 
			for (int j = 0; j < I; j++)
				qki[i][j] = Math.random() / 10;
		
		for (int i = 0; i < U; i++) 
			bu[i] = Math.random() / 10;
		
		for (int i = 0; i < I; i++)
			bi[i] = Math.random() / 10;
		
		double total = 0.0;
		int cnt = 0;
		Iter it = mat.iterateMatrix();
		Double sample;
		while (!(sample = it.next()).equals(Double.MIN_VALUE)) {
			total += sample;
			cnt ++;
		}
		averRate = total / cnt;
	}
	
	public double predictVal(int u, int i) {
		return averRate + bu[u] + bi[i] + innerProduct(u, i);
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
//			
//			if (puk[u][k].isNaN() || qki[k][i].isNaN() || puk[u][k].isInfinite() || qki[k][i].isInfinite()) {
//				
//				System.out.println("oldpuk: " + oldpuk);
//				System.out.println("oldqki: " + oldqki);
//				System.out.println("step: " + step);
//				System.out.println("lamda: " + lamda);
//				System.out.println( oldpuk + step * ( err * oldqki - lamda * puk[u][k]) );
//				
//				System.out.println(puk[u][k] + " " + qki[k][i]);
//				
//				try {
//					Thread.sleep(100000);
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			}
			
//			System.out.println(puk[u][k] + " " + qki[k][i]);
		}
		bu[u] = bu[u] + step*(err - lamda*bu[u]);
		bi[i] = bi[i] + step*(err - lamda*bi[i]);
		
//		System.out.println(bu[u] + " " + bi[i] + "---");
	}
	
	private double error(int u, int i, double trueVal, RatingMatrix mat) {
//		System.out.println("bu[u] : " + bu[u]);
//		System.out.println("bu[i] : " + bu[i]);
		
		double eval = averRate + bu[u] + bi[i] + innerProduct(u, i);
		if (eval < mat.getMinRate()) eval = mat.getMinRate();
		if (eval > mat.getMaxRate()) eval = mat.getMaxRate();
		
//		System.out.println("eval : " + eval);
		
		return trueVal - eval;
	}
	
	private double innerProduct(int u, int i) {
		double ret = 0.0;
		for (int j = 0; j < K; j++) {
//			System.out.println("K : " + j);
//			System.out.println("puk[u][j] : " + puk[u][j]);
//			System.out.println("qki[j][i] : " + qki[j][i]);
			
//			if (puk[u][j].isNaN() || qki[j][i].isNaN()) {
//				try {
//					Thread.sleep(100000);
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			}
			
			ret += puk[u][j] * qki[j][i];
			
//			System.out.println("ret: " + ret);
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
		
		double user = 0.0;
		for (int i = 0; i < mat.getX(); i++)
			user += bu[i] * bu[i];
		
//		System.out.println("user :" + user);
		
		double item = 0.0;
		for (int i = 0; i < mat.getY(); i++)
			item += bi[i] * bi[i];
		
//		System.out.println("item: " + item);
		
		return 0.5 * cost + 0.5 * lamda * (p + q + user + item);
	}
}
