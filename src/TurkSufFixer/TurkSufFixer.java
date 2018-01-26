package TurkSufFixer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.regex.*;


public class TurkSufFixer {
	
	public  static final List<String> suffixes = Arrays.asList(Suffixes.ACC,Suffixes.DAT,Suffixes.LOC,
															   Suffixes.ABL,Suffixes.INS,Suffixes.PLU, Suffixes.GEN);
    private static final String vowels = "aıuoeiüö";
    //private static final String backvowels = "aıuo";
    private static final String frontvowels = "eiüö";
    private static final String backunrounded = "aı";
    private static final String backrounded = "uo";
    private static final String frontunrounded = "ei";
    private static final String frontrounded = "üö";
    private static final String roundedvowels = "uoüö";
    private static final String hardconsonant = "fstkçşhp";
    private static final String H = "ıiuü";
    private static final String[] numbers = {"sıfır","bir","iki","üç","dört","beş","altı","yedi","sekiz","dokuz" };
    private static final String[] tens = {"sıfır", "on", "yirmi", "otuz", "kırk", "elli", "altmış", "yetmiş", "seksen", "doksan" };
    private static Hashtable<String, String> superscript;
    private static Hashtable<Integer,String> digits;
    private static List<StringTuple> consonantTuple;
    private static List<StringTuple> translate_table;
    private static List<StringTuple> accent_table;
    private static HashSet<String> dictionary;
    private static HashSet<String> possesive;
    private static HashSet<String> exceptions;
    private static HashSet<String> haplology;
    private static Hashtable<String, String> others;
    private static Pattern time_pattern;
    private static final Locale turkishCulture = Locale.forLanguageTag("tr-TR");
    private boolean updated = false;
    private String posspath   = "sozluk/iyelik.txt";
    static
    {
        digits = new Hashtable<Integer, String>();
        digits.put(0, "yüz");
        digits.put(3, "bin");
        digits.put(6, "milyon");
        digits.put(9, "milyar");
        digits.put(12, "trilyon");
        digits.put(15, "katrilyon");
        superscript = new Hashtable<String,String>();
        superscript.put("²", "kare");
        superscript.put("³", "küp");
    }
    public TurkSufFixer() throws SuffixException{
    	String dictpath   = "sozluk/kelimeler.txt";
    	String exceptpath = "sozluk/istisnalar.txt";
    	String haplopath  = "sozluk/unludusmesi.txt";  	
    	String othpath    = "sozluk/digerleri.txt";
    	time_pattern = Pattern.compile("([01]?[0-9]|2[0-3])[.:]00", Pattern.UNICODE_CHARACTER_CLASS);
    	consonantTuple = new ArrayList<StringTuple>(5);
    	consonantTuple.add(new StringTuple("ğ","k"));
    	consonantTuple.add(new StringTuple("g","k"));
    	consonantTuple.add(new StringTuple("d","t"));
    	consonantTuple.add(new StringTuple("c","ç"));
    	consonantTuple.add(new StringTuple("b","p"));
    	translate_table = new ArrayList<StringTuple>(5);
    	translate_table.add(new StringTuple("ae","A"));
    	translate_table.add(new StringTuple(H,"H"));
    	accent_table = new ArrayList<StringTuple>(4);
    	accent_table.add(new StringTuple("â","e"));
    	accent_table.add(new StringTuple("î","i"));
    	accent_table.add(new StringTuple("û","ü"));
    	accent_table.add(new StringTuple("ô","ö"));
    	try {
			dictionary = new HashSet<String>(Files.readAllLines(Paths.get(dictpath)));
			possesive  = new HashSet<String>(Files.readAllLines(Paths.get(posspath)));
			exceptions = new HashSet<String>(Files.readAllLines(Paths.get(exceptpath)));
			haplology  = new HashSet<String>(Files.readAllLines(Paths.get(haplopath)));
			dictionary.addAll(exceptions);
			dictionary.addAll(haplology);
			others = new Hashtable<String,String>();
			Pattern p = Pattern.compile("(\\w+) +-> +(\\w+)",Pattern.UNICODE_CHARACTER_CLASS);
			Matcher m;
			for(String line: Files.readAllLines(Paths.get(othpath))){
				line = turkishSanitize(line);
				m = p.matcher(line);
				if (m.find()){
					others.put(m.group(1), m.group(2));
				}
				else{
					
					others.put(line, line + (line.endsWith("k") ? 'a' : 'e'));
				}
			}
		} catch (IOException e) {
			throw new DictionaryNotFound(e.getMessage());
		}  
    	
    }

    private String readNumber(String number){
    	Matcher m = time_pattern.matcher(number);
    	if (m.find()){
    		number = m.group(1);
    	}
    	int len = number.length();
    	for(int i = len - 1; i >=0; i-- ){
    		if(number.charAt(i) != '0' && isNumber(Character.toString(number.charAt(i)))){
    			int n = Character.getNumericValue(number.charAt(i));
    			i = len - i - 1;
    			if (i == 0)
    				return numbers[n];
    			else if (i == 1)
    				return tens[n];
    			else
    			{
    				n = (i / 3) * 3;
    				n = n > 15 ? 15 : n;
    				return digits.get(n);
    			}
    		}
    	}
    	return "sıfır";
    }
    
    private List<StringTuple> divideWord(String name, String suffix){
		String realsuffix = name.substring(name.length() - suffix.length());
		name = suffix.length() > 0 ? name.substring(0,name.length() - suffix.length()) : name;
		List<StringTuple> result = new ArrayList<StringTuple>();
		if (dictionary.contains(name) || checkConsonantHarmony(name,suffix)){
			result.add(new StringTuple("",name));
		}
		else{
			String realname = checkEllipsisAffix(name,realsuffix);
			if (!"".equals(realname)) result.add(new StringTuple("",realname));
		}
		for (int i = 2; i < name.length() - 1; i++){
			String firstWord = name.substring(0,i);
			String secondWord = name.substring(i);
			if(dictionary.contains(firstWord)){
				if(dictionary.contains(secondWord) || checkConsonantHarmony(secondWord,suffix))
					result.add(new StringTuple(firstWord,secondWord));
				else
				{
					secondWord = checkEllipsisAffix(secondWord,realsuffix);
					if (!"".equals(secondWord)) result.add(new StringTuple(firstWord,secondWord));
				}
			}
		}
    	return result;
    }
    
    private String checkEllipsisAffix(String name, String realsuffix){
    	if (!H.contains(realsuffix)) return "";
    	name = name.substring(0, name.length() - 1) + realsuffix + name.substring(name.length()-1);
    	return haplology.contains(name) ? name : "";
    }
    
    private boolean checkConsonantHarmony(String name, String suffix){
    	if("H".equals(suffix)){
    		for(StringTuple cTuple : consonantTuple){
    			if (name.substring(name.length() - 1).equals(cTuple.first) &&
    				dictionary.contains(name.substring(0,name.length() -1) + cTuple.second)){
    				return true;
    			}
    		}
    	}
    	return false;
    }
    
    private boolean checkVowelHarmony(String name, String suffix){
    	String lastVowelOfName = "", firstVowelOfSuffix = "";
    	boolean isFrontVowel = false;
    	if (exceptions.contains(name))
    		isFrontVowel = true;

    	lastVowelOfName = Character.toString(name.charAt(findLastVowel(name)));
    	for(int i = 0; i < suffix.length(); i++){
    		String letter = suffix.substring(i,i+1);
    		if(vowels.contains(letter)){
    			firstVowelOfSuffix = letter;
    			break;
    		}
    	}
    	boolean frontness = (frontvowels.contains(lastVowelOfName) || isFrontVowel) == frontvowels.contains(firstVowelOfSuffix);
    	boolean roundness = roundedvowels.contains(lastVowelOfName) == roundedvowels.contains(firstVowelOfSuffix);
    	return frontness && (roundness || !H.contains(firstVowelOfSuffix));
    			
    }
    
    private String surfacetolex(String suffix){
    	for(StringTuple tTuple : translate_table){
    		for(int i = 0; i < tTuple.first.length(); i++){
    			suffix = suffix.replace(tTuple.first.charAt(i), tTuple.second.charAt(0));
    		}
    	}
    	return suffix;
    }
    private boolean checkCompoundNoun(String name){
    	if (name.endsWith("oğlu")){
    		return true;
    	}
    	Hashtable<String,String> probablesuff = new Hashtable<String,String>(4);
    	String temp;
    	int len = name.length();
    	for(int i = 1; i < 5 && i < len; i++){
    		temp = name.substring(len - i);
    		probablesuff.put(surfacetolex(temp),temp);
    	}
   
    	for(String posssuff : Arrays.asList("lArH","H","yH","sH", "lHğH")){
    		String realsuffix = probablesuff.get(posssuff);
    		if(realsuffix != null){
    			List<StringTuple> wordpairs = divideWord(name,posssuff);
    			for(StringTuple wordpair : wordpairs){
    				if(checkVowelHarmony(wordpair.second,realsuffix)){
    					updated = true;
    					possesive.add(name);
    					return true;
    				}
    			}
    		}
    	}
    	return false;
    }
    
    private boolean checkExceptionalWord(String name){
    	for(StringTuple word : divideWord(name,"")){
    		if(!"".equals(word.first) && exceptions.contains(word.second))
    			return true;
    	}
    	return false;
    }
    
    public String makeAccusative(String name) throws SuffixException{
    	return constructName(name, Suffixes.ACC, true);
    }
    
    public String makeAccusative(String name, Boolean apostrophe) throws SuffixException{
    	return constructName(name, Suffixes.ACC, apostrophe);
    }
    
    
    public String makeDative(String name) throws SuffixException{
    	return constructName(name, Suffixes.DAT, true);
    }
    
    public String makeDative(String name, Boolean apostrophe) throws SuffixException{
    	return constructName(name, Suffixes.DAT, apostrophe);
    }
    
    public String makeLocative(String name) throws SuffixException{
    	return constructName(name, Suffixes.LOC, true);
    }
    
    public String makeLocative(String name, Boolean apostrophe) throws SuffixException{
    	return constructName(name, Suffixes.LOC, apostrophe);
    }
    
    public String makeGenitive(String name) throws SuffixException{
    	return constructName(name, Suffixes.GEN, true);
    }
    
    public String makeGenitive(String name, Boolean apostrophe) throws SuffixException{
    	return constructName(name, Suffixes.GEN, apostrophe);
    }
    
    public String makeAblative(String name) throws SuffixException{
    	return constructName(name, Suffixes.ABL, true);
    }
    
    
    public String makeAblative(String name, Boolean apostrophe) throws SuffixException{
    	return constructName(name, Suffixes.ABL, apostrophe);
    }
    
    public String makeInstrumental(String name) throws SuffixException{
    	return constructName(name, Suffixes.INS, true);
    }
    
    public String makeInstrumental(String name, Boolean apostrophe) throws SuffixException{
    	return constructName(name, Suffixes.INS, apostrophe);
    }
    
    public String makePlural(String name) throws SuffixException{
    	return constructName(name, Suffixes.PLU, true);
    }
    
    public String makePlural(String name, Boolean apostrophe) throws SuffixException{
    	return constructName(name, Suffixes.PLU, apostrophe);
    }
    
    private String constructName(String name, String suffix, Boolean apostrophe) throws SuffixException{
    	return String.format("%s%s%s", name, apostrophe ? "'": "", getSuffix(name,suffix));
    }
    
    public String getSuffix(String name, String suffix) throws SuffixException{
    	// TODO: bir kere böl bir kere contain kontrolü yap
    	name = turkishSanitize(name);
    	if (name.isEmpty()){
    		throw new EmptyNameException();
    	}
    	if (!suffixes.contains(suffix)){
    		throw new NotValidSuffixException();
    	}
    	String rawsuffix = suffix;
    	boolean soft = false;
    	String[] words = name.trim().split(" ");
    	name = words[words.length - 1];
    	String lastLetter = name.substring(name.length() - 1);
    	if(H.contains(lastLetter) && (!rawsuffix.equals(Suffixes.INS) && !rawsuffix.equals(Suffixes.PLU)) && 
    	  (words.length > 1 || !dictionary.contains(name)) && (possesive.contains(name) || checkCompoundNoun(name)))
    	{
    		suffix = 'n' + suffix;
    	}
    	else if (isNumber(lastLetter)){
    		name = readNumber(name);
    		lastLetter = name.substring(name.length() - 1);
    	}
    	else if(exceptions.contains(name) || (!dictionary.contains(name) && checkExceptionalWord(name))){
    		soft = true;
    	}
    	else if (others.containsKey(name)){
    		name = others.get(name);
    		lastLetter = name.substring(name.length() - 1);
    	}
    	else if (superscript.containsKey(lastLetter)){
    		name = superscript.get(lastLetter);
    		lastLetter = name.substring(name.length() - 1);
    	}
    	String lastVowel;
    	int i = findLastVowel(name);
    	if (i == -1){
    		lastVowel  = name.endsWith("k") ? "a" : "e";
    		lastLetter = name.endsWith("k") ? "a" : "e";
    	}
    	else
    	{
    		lastVowel = Character.toString(name.charAt(i));
    	}
    	if (suffix.contains("H")){
    		suffix = suffix.replace('H', findReplacement(lastVowel, soft));
    		if (rawsuffix.equals(Suffixes.GEN) && 'n' != suffix.charAt(0) && vowels.contains(name.substring(name.length() - 1))){
    			suffix = "n" + suffix;
    		}
    	}
    	else
    	{
    		if(frontvowels.contains(lastVowel) || soft){
    			suffix = suffix.replace('A', 'e');
    		}
    		else{
    			suffix = suffix.replace('A', 'a');
    		}
    		if(hardconsonant.contains(lastLetter)){
    			suffix = suffix.replace('D', 't');
    		}
    		else{
    			suffix = suffix.replace('D', 'd');
    		}	
    		
    		
    	}
    	
    	if (vowels.contains(lastLetter) && 
    	   (vowels.contains(suffix.substring(0,1)) || rawsuffix.equals(Suffixes.INS))){
    		suffix = 'y' + suffix;
    	}
    	return suffix;
    }
    
    private char findReplacement(String lastVowel, boolean soft){
    	if (frontrounded.contains(lastVowel) || (soft && backrounded.contains(lastVowel))){
    		return 'ü';
    	}
    	else if(frontunrounded.contains(lastVowel) || (soft && backunrounded.contains(lastVowel))){
    		return 'i';
    	}
    	else if (backrounded.contains(lastVowel)){
    		return 'u';
    	}
    	return 'ı';
    }
    
    private boolean isNumber(String number){
    	return number.matches("-?\\d+(\\.\\d+)?");
    }
    
    private int findLastVowel(String name){
    	for (int i = name.length() - 1; i >= 0; i--){
    		if(vowels.contains(name.substring(i,i+1))){
    			return i;
    		}
    	}
    	return -1;
    }
    
    public void closeDictionary() throws IOException{
    	if (updated)
    		Files.write(Paths.get(posspath), possesive);
    }
    
    private String turkishSanitize(String name){
    	name = name.toLowerCase(turkishCulture);
    	for (StringTuple tTuple : accent_table){
    		name = name.replace(tTuple.first, tTuple.second);
    	}
    	return name.trim();
    }
    
    public class SuffixException extends Exception{
		private static final long serialVersionUID = 1L;
    }
    class EmptyNameException extends SuffixException{
		private static final long serialVersionUID = 1L;
    }
    class NotValidSuffixException extends SuffixException{
		private static final long serialVersionUID = 1L;
    }
    class DictionaryNotFound extends SuffixException{
		private static final long serialVersionUID = 1L;
		String message;
	    public DictionaryNotFound(String message){
	    	this.message = message;
	    }
	    @Override
	    public String getMessage() {
	        return message;
		}
    }
    class StringTuple{ 
    	public String first;
    	public String second;
    	StringTuple(String x, String y){
    		first = x;
    		second = y;
    	}
    }
    class Suffixes{
    	public static final String ACC = "H";
        public static final String DAT = "A";
        public static final String LOC = "DA";
        public static final String ABL = "DAn";
        public static final String GEN = "Hn";
        public static final String INS = "lA";
        public static final String PLU = "lAr";
        
    }
}


 
