import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

/* Talha BAYBURTLU - 150118066
 * This program takes signed,unsigned and floating point numbers
 * from input.txt and then converts them to hexadecimal numbers. */

public class talhabayburtlu {
	private static int floatingPointSize = 0; 
	private static boolean bigEndian = false;
	
	public static void main(String[] args) {
		File inFile = new File("input.txt");
		Scanner inFileScanner = null;
		BufferedWriter writer = null;
		try { 
			inFileScanner = new Scanner(inFile); // Opening input.txt file.
		} catch(Exception ex) { 
			System.out.println("input.txt couldn't find!");
			System.exit(0);
		}
		
		
		LinkedHashMap<Double,String> values = readFile(inFileScanner); // Storing inputs in linked hash map.
		prompInput(); // Taking floating point size and byte ordering.
		
		try {
			writer = new BufferedWriter(new FileWriter("output.txt")); // Opening output.txt file.
		} catch (IOException e) {
			System.out.println("output.txt couldn't create!");
			System.exit(-1);
		}
		
		for (Map.Entry<Double,String> element : values.entrySet()) { // For every element in map calling corresponding method.
			try {
				if (element.getValue() == "S")
					writer.append(convertSigned(element.getKey().intValue()) + "\n");
				else if (element.getValue() == "U")
					writer.append(convertUnsigned(element.getKey().intValue()) + "\n");
				else
					writer.append(convertFloatingPoint(element.getKey()) + "\n");
			} catch (IOException e) {
				System.out.println("IOException");
				System.exit(-2);
			}		
		}
		
		System.out.println("Output created at output.txt");
		
		try {
			writer.close();
		} catch (IOException e) {
			System.out.println("File could not close.");
		}
	}
	
	public static LinkedHashMap<Double,String> readFile(Scanner inFileScanner) { // Reads file from line by line and stores them in map.
		LinkedHashMap<Double,String> inputs = new LinkedHashMap<Double,String>();
		
		while (inFileScanner.hasNextLine()) {
			String currentLine = inFileScanner.nextLine();
			if (currentLine.contains(".")) // Floating Point Number case.
				inputs.put(Double.parseDouble(currentLine), "D");
			else if (currentLine.endsWith("U") || currentLine.endsWith("u")) // Unsigned number case.
				inputs.put(Double.parseDouble(currentLine.substring(0,currentLine.length() - 1)), "U");
			else // Signed case.
				inputs.put(Double.parseDouble(currentLine), "S");
		}
		
		return inputs;
	}
	
	public static void prompInput() { // Takes floating point size and byte ordering.
		Scanner inputScanner = new Scanner(System.in);
		
		System.out.println("Byte ordering (Choose 1 for Little Endian, 2 for Big Endian): ");
		int byteOrdering = inputScanner.nextInt();
		while (byteOrdering != 1 && byteOrdering != 2) {
			System.out.println("Wrong input! Byte ordering: (Choose 1 for Little Endian, 2 for Big Endian)");
			byteOrdering = inputScanner.nextInt();
		}
		bigEndian = byteOrdering == 2;
		
		System.out.println("Floating point size (Choose 1,2,3,4): ");
		int floatingPointSizeInput = inputScanner.nextInt();
		while (floatingPointSizeInput < 1 || floatingPointSizeInput > 4) {
			System.out.println("Wrong input! Floating point size (Choose 1,2,3,4): ");
			floatingPointSizeInput = inputScanner.nextInt();
		}
		floatingPointSize = floatingPointSizeInput;
	}
	
	public static String convertUnsigned(int num) { // Converts unsigned number to hexadecimal number.
		if (num <= 0)
			return num + "can not be unsigned and negative at the same time.";
		
		String binaryNum = convertDecimalToBinary(num); // Converting decimal to binary.
		return convertBigOrLittleEndian(convertBinaryToHex(binaryNum)); // Returning binary to hexadecimal number based on big or little endian.
	}
	
	public static String convertSigned(int num) {
		String binaryNum = convertDecimalToBinary(num); // Converting decimal to binary.
		return convertBigOrLittleEndian(convertBinaryToHex(binaryNum)); // Returning binary to hexadecimal number based on big or little endian.
	}
	
	public static String convertFloatingPoint(double num) { // Converts floating point number to IEFF number.
		String mantissaOld = convertFloatingToBinary(num); // Converting floating point to binary number.
		String mantissaUpdated = "";
		
		int e = mantissaOld.charAt(0) == '1' ? mantissaOld.indexOf('.') - 1 : -1 * (mantissaOld.indexOf("1") - 1);
		int expNum = e + ((int) Math.pow(2, 1 + 2 * floatingPointSize)) - 1;
		
		String exponent = convertDecimalToBinary(expNum).substring(4 * floatingPointSize); // Trimming exponent part.
		exponent = expNum != 0 ? exponent.substring(exponent.indexOf('1')) : exponent;
		
		if (mantissaOld.charAt(0) == '1')
			mantissaUpdated = mantissaOld.substring(0,1) + "." + mantissaOld.substring(1,e + 1) + mantissaOld.substring(e + 2);
		else {
			mantissaUpdated = e != -2 ? mantissaOld.charAt(-1 * e + 1) + "." + mantissaOld.substring(-1 * e + 2): "0.0";
		}
		
		if (expNum == 0) // Checking denormalized or not
			mantissaUpdated = "0." + mantissaUpdated.charAt(0) + mantissaUpdated.substring(2);
		
		for (int i = mantissaUpdated.length() ; i < 6 * floatingPointSize - 1; i++)
			mantissaUpdated += "0";
		
		char[] mantissaArr = mantissaUpdated.toCharArray();
		int mantissaRoundIndex = floatingPointSize <= 2 ? 6 * floatingPointSize - 2 : 14;
		if (mantissaArr.length > mantissaRoundIndex + 1 && mantissaArr[mantissaRoundIndex + 1] == '1') { // Checking rounding is necessary or not.
			for (int i = mantissaRoundIndex ; i > 1 ; i--) {
				if (mantissaArr[i] == '1') // Adding one to left part (including last bit).
					mantissaArr[i] = '0';
				else {
					mantissaArr[i] = '1';
					break;
				}
			}
			
			for (int i = mantissaRoundIndex + 1 ; i < mantissaArr.length ; i++) // Filling last bits with 0 if there are missing bits.
				mantissaArr[i] = '0';
			
			boolean overflow = true;
			for (int i = 2; i < mantissaArr.length ; i++) // Checking overflow exists or not.
				if (mantissaArr[i] == '1') {
					overflow = false;
					break;
				}
			
			mantissaArr[2] = overflow ? 1 : mantissaArr[2]; // Based on overflow, first bit of mantissa becomes 1.
			mantissaUpdated = String.valueOf(mantissaArr);
		} 
		mantissaUpdated = mantissaUpdated.substring(2); // Trimming the first two indexes.

		String ieee = "" + (num >= 0 ? 0 : 1) + exponent + mantissaUpdated; // Creating IEEE number with signed bit, exponent part and mantissa part.
		
		return convertBigOrLittleEndian(convertBinaryToHex(ieee)); // Returning IEEE number to hexadecimal number based on byte ordering.
	}
	
	public static String convertDecimalToBinary(int num) { // Converts decimal number to binary number.
		int binaryNum[] = new int[8 * floatingPointSize];
		int absNum = Math.abs(num);
		
		int index = 8 * floatingPointSize - 1; 
		while (absNum > 0) { // Binary number creating by dividing decimal number into two.
			binaryNum[index] = (absNum%2);
			absNum /= 2;
			index--;
		}
		
		if (num < 0) { // Checking number is negative or not.
			for (int i = 0; i < binaryNum.length ; i++) // Flipping all bits.
				binaryNum[i] = binaryNum[i] == 1 ? 0 : 1;
			
			for (int i = binaryNum.length - 1; i >= 0 ; i++) { // Adding one to flipped bits.
				if (binaryNum[i] + 1 % 2 == 0)
					binaryNum[i] = 0;
				else {
					binaryNum[i] = 1;
					break;
				}
			}	
		}
		
		String binaryNumStr[] = new String[binaryNum.length];
		for (int i = 0; i < binaryNumStr.length ; i++) 
			binaryNumStr[i] = binaryNum[i] + "";
		
		return String.join("", binaryNumStr); // Returning binary number from joining array.
	}
	
	public static String convertFloatingToBinary(double num) { // Converts floating point number to binary.
		String binaryNum = "";
		int absNum = (int)(Math.abs(num));
		
		while (absNum >= 1) { // Creating binary's left part from the dot.
			binaryNum =  "" + (absNum%2) + binaryNum;
			absNum /= 2;
		}
		
		if (binaryNum.equals(""))
			binaryNum += "0";
		
		binaryNum += ".";
		
		double pointPart = Math.abs(num) - (int)(Math.abs(num));
		
		
		for (int i = -1 ; pointPart > 0 ; i--) { // Creating binary's right part from the dot.
			if (pointPart - Math.pow(2 , i) >= 0) {
				binaryNum += "1";
				pointPart -= Math.pow(2 , i);
			} else
				binaryNum += "0";
		}
		return binaryNum;
	}
	
	public static String convertBinaryToHex(String bits) { // Converts binary number to hexadecimal number.
		String hex = "";
		
		for (int startIndex = 0, endIndex = 4; startIndex < 8 * floatingPointSize ; startIndex += 4 , endIndex += 4) { // Partitioning binary number by 4 bits.
			String subBits = bits.substring(startIndex, endIndex);
			// Calculating value of current 4 bits.
			int value = ((int)subBits.charAt(0) - 48) * 8 +  ((int)subBits.charAt(1) - 48) * 4 + ((int)subBits.charAt(2) - 48) * 2 + ((int)subBits.charAt(3) - 48) * 1;
			
			if (value < 10) // Adding hexadecimal string corresponding character.
				hex += value + "";
			else if (value == 10)
				hex += "A";
			else if (value == 11)
				hex += "B";
			else if (value == 12)
				hex += "C";
			else if (value == 13)
				hex += "D";
			else if (value == 14)
				hex += "E";
			else
				hex += "F";
			
			if (endIndex % 8 == 0 && endIndex != 8 * floatingPointSize) // Adding space if current 4 bits is not start or end bits.
				hex += " ";
		}
		
		return hex;
	}
	
	public static String convertBigOrLittleEndian(String hex) { // Converting big or little endian.
		if (!bigEndian) {
			String converted = "";
			for (int i = 3 * floatingPointSize - 1 ; i > 0 ; i -= 3) { // Reversing hexadecimal number by 2 character and 1 space.
				converted += hex.substring(i-2 , i);
				converted += i > 2 ? " " : "";
			}
			return converted;
		} else
			return hex;
	}
}
