//package TurkSufFixer;
import TurkSufFixer.TurkSufFixer.SuffixException;
import static org.junit.Assert.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import TurkSufFixer.TurkSufFixer;
import java.util.*;
import org.junit.Before;
import org.junit.Test;

public class UnitTest {
	HashMap<String, List<String>> simplewords = new HashMap<String, List<String>>();
	HashMap<String, List<String>> numbers     = new HashMap<String, List<String>>();
	HashMap<String, List<String>> exceptions  = new HashMap<String, List<String>>();
	HashMap<String, List<String>> consonant   = new HashMap<String, List<String>>();
	HashMap<String, List<String>> possesive   = new HashMap<String, List<String>>();
	HashMap<String, List<String>> others      = new HashMap<String, List<String>>();
	HashMap<String, HashMap<String, List<String>>> test_list = new HashMap<String, HashMap<String, List<String>>>();
	@Before
	public void setUp() throws Exception {
		test_list.put("simplewords", simplewords);
		test_list.put("numbers", numbers);
		test_list.put("exceptions", exceptions);
		test_list.put("consonantharmony", consonant);
		test_list.put("possesive", possesive);
		test_list.put("others", others);
		for (Map.Entry<String, HashMap<String, List<String>>> entry : test_list.entrySet()){
			String path = "test/tests/"+entry.getKey();
			HashMap<String, List<String>> tempTable = entry.getValue();
			for(String line: Files.readAllLines(Paths.get(path))){
				if (line.trim().length() == 0) continue;
				String[] split = line.trim().split("=");
				String name = split[0].trim();
				String[] suffixes = split[1].trim().substring(1,split[1].length() - 2).split(",");
				tempTable.put(name, Arrays.asList(suffixes));
			}	
		
		}
	}
	@Test
	public void simpleWordTest(){
		try{
			baseTest(simplewords);
		}catch(assertException e){
			assertEquals(true,e.getMessage());
		}
	}
	@Test
	public void numberTest(){
		try{
			baseTest(numbers);
		}catch(assertException e){
			assertEquals(true,e.getMessage());
		}
	}
	@Test
	public void exceptionTest(){
		try{
			baseTest(exceptions);
		}catch(assertException e){
			assertEquals(true,e.getMessage());
		}
	}
	@Test
	public void consonantTest(){
		try{
			baseTest(consonant);
		}catch(assertException e){
			assertEquals(true,e.getMessage());
		}
		
	}
	@Test
	public void possesiveTest(){
		try{
			baseTest(possesive);
		}catch(assertException e){
			assertEquals(true,e.getMessage());
		}
	}
	@Test
	public void othersTest(){
		try{
			baseTest(others);
		}catch(assertException e){
			assertEquals(true,e.getMessage());
		}
	}
	
	private void baseTest(HashMap<String,List<String>> list) throws assertException {
		List<String> suffixList = TurkSufFixer.suffixes;
		try{
			TurkSufFixer ekle = new TurkSufFixer();
			for (Map.Entry<String,List<String>> entry : list.entrySet()){
				String name = entry.getKey();
				List<String> correctsf = entry.getValue();
				for(int i = 0; i < correctsf.size(); i++){
					String rt = null;	
					rt = ekle.getSuffix(name, suffixList.get(i));
					if (!rt.equals(correctsf.get(i))){
						throw new assertException(name,correctsf.get(i),rt);
					}
				}
			}
		}
		catch (SuffixException e){
			throw new assertException("","","");
		}
		
	}
	public class assertException extends Exception {
	    private static final long serialVersionUID = 1L;
	    String name;
	    String correct;
	    String rtn;
	    public assertException(String name,String correct,String rtn){
	    	this.name = name;
	    	this.correct = correct;
	    	this.rtn = rtn;
	    }
	    @Override
	    public String getMessage() {
	        return String.format("'%s' için '%s' dönmesi gerekirken '%s' döndü.", name, correct,rtn);
	    }
	}

}
