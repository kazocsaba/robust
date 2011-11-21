package hu.kazocsaba.robust;

import java.util.List;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Kazó Csaba
 */
public class RansacTest {
	@Test
	public void test1() throws NoModelFoundException {
		List<Integer> data=Arrays.asList(8, 8, 8, 8, 8, 1, 4);
		
		Ransac<Integer, Integer> ransac=new Ransac<Integer, Integer>(0);
		Fitter<Integer, Integer> integerFitter = new Fitter<Integer, Integer>(1) {

			@Override
			public double getError(Integer model, Integer datum) {
				return Math.abs(model - datum);
			}

			@Override
			public Integer computeModel(List<Integer> data) {
				int sum = 0;
				for (int d : data)
					sum += d;
				return sum / data.size();
			}
		};
		
		for (int repeats=0; repeats<100; repeats++) {
			int result=ransac.perform(integerFitter, data, null);
			assertEquals(8, result);
		}
		
		// all inliers
		data=Collections.nCopies(100, 8);
		for (int repeats=0; repeats<100; repeats++) {
			int result=ransac.perform(integerFitter, data, null);
			assertEquals(8, result);
		}
	}
}
