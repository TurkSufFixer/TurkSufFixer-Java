package TurkSufFixer;

import java.io.IOException;
import java.util.List;

import TurkSufFixer.TurkSufFixer.SuffixException;

public class Test {

	public static void main(String[] args) {
		try {
			TurkSufFixer t = new TurkSufFixer();
			String name = "TBMM";
			List<String> suffixes = TurkSufFixer.suffixes;
			for(String suffix : suffixes){
				System.out.println(name + t.getSuffix(name, suffix));
			}
				t.closeDictionary();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SuffixException e){
			e.printStackTrace();
		}
	}

}
