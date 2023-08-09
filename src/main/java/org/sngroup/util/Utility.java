/*
 * Atomic Predicates Verifier
 *
 * Copyright (c) 2013 UNIVERSITY OF TEXAS AUSTIN. All rights reserved. Developed
 * by: HONGKUN YANG and SIMON S. LAM http://www.cs.utexas.edu/users/lam/NRL/
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * with the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimers.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimers in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the UNIVERSITY OF TEXAS AUSTIN nor the names of the
 * developers may be used to endorse or promote products derived from this
 * Software without specific prior written permission.
 *
 * 4. Any report or paper describing results derived from using any part of this
 * Software must cite the following publication of the developers: Hongkun Yang
 * and Simon S. Lam, Real-time Verification of Network Properties using Atomic
 * Predicates, IEEE/ACM Transactions on Networking, April 2016, Volume 24, No.
 * 2, pages 887-900 (first published March 2015, Digital Object Identifier:
 * 10.1109/TNET.2015.2398197).
 *
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * CONTRIBUTORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS WITH
 * THE SOFTWARE.
 */

package org.sngroup.util;

import org.sngroup.verifier.TSBDD;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

public class Utility {
	private static int IDCount = 0;
	private static Vector<Integer> ZeroVector = null;
	private static final Logger logger = Logger.getLogger("log");



	public static String getBase64FromInputStream(InputStream in) {
		// 将图片文件转化为字节数组字符串，并对其进行Base64编码处理
		byte[] data = null;
		// 读取图片字节数组
		try {
			ByteArrayOutputStream swapStream = new ByteArrayOutputStream();
			byte[] buff = new byte[100];
			int rc = 0;
			while ((rc = in.read(buff, 0, 100)) > 0) {
				swapStream.write(buff, 0, rc);
			}
			data = swapStream.toByteArray();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return new String(Base64.getEncoder().encode(data));
	}

	public static Vector<Integer> getOneNumVector(int num){
		Vector<Integer> t = new Vector<>(1);
		t.add(num);
		return t;
	}

	public static double avg(List<Long> list){
		double res = 0;
		double count = list.size();
		for (Long aLong : list) {
			res += (double)aLong / count;
		}
		return res;
	}

	public static double avgDouble(List<Double> list){
		double res = 0;
		double count = list.size();
		for (Double aLong : list) {
			res += aLong / count;
		}
		return res;
	}

	public static double avgWithoutZero(List<Long> list){
		double res = 0;
		double count = countNotZero(list);
		for (Long aLong : list) {
			if(aLong==0L) continue;
			res += aLong / count;
		}
		return res;
	}

	public static int countNotZero(List<Long> list) {
		int res = 0;
		for (Long aLong : list) {
			if(aLong==0L) continue;
			res++;
		}
		return res;
	}
	/*
	 * only used to compute no more than 2^16
	 */
	public static long Power2(int exponent)
	{
		if(exponent <=16)
		{
			switch(exponent){
				case 0: return 1;
				case 1: return 2;
				case 2: return 4;
				case 3: return 8;
				case 4: return 16;
				case 5: return 32;
				case 6: return 64;
				case 7: return 128;
				case 8: return 256;
				case 9: return 512;
				case 10: return 1024;
				case 11: return 2048;
				case 12: return 4096;
				case 13: return 8192;
				case 14: return 16384;
				case 15: return 32768;
				case 16: return 65536;
				default: System.err.println("exponent is too large!");
					break;
			}
		}
		else
		{
			long power = 1;
			for(int i = 0; i < exponent; i ++)
			{
				power = power * 2;
			}
			return power;
		}
		// should not be here
		return 0;
	}

	public static int tailingZeros(long num, int bits)
	{
		if(num == 0) return bits;
		int tmptailing = 0;
		for(int i = 0; i <bits; i ++)
		{
			long tester = Power2(i) - 1;
			long tn = num & tester;
			if(tn == 0)
			{
				tmptailing = i;
			}else
			{
				break;
			}

		}
		return tmptailing;
	}

	/**
	 * return the binary representation of num
	 * e.g. num = 10, bits = 4, return an array of {0,1,0,1}
	 */
	public static int[] CalBinRep(long num, int bits)
	{
		if(bits == 0) return new int[0];

		int [] binrep = new int[bits];
		long numtemp = num;
		for(int i = bits; i >0; i--)
		{
			long abit = numtemp & Power2(i - 1);
			if(abit == 0)
			{
				binrep[i - 1] = 0;
			}else
			{
				binrep[i - 1] = 1;
			}
			numtemp = numtemp - abit;
		}
		return binrep;
	}


	public static int ip2Int(String ipString) {
		// 取 ip 的各段
		String[] ipSlices = ipString.split("\\.");
		int rs = 0;
		for (int i = 0; i < ipSlices.length; i++) {
			// 将 ip 的每一段解析为 int，并根据位置左移 8 位
			int intSlice = Integer.parseInt(ipSlices[i]) << (8 * (ipSlices.length - i -1));
			// 求与
			rs = rs | intSlice;
		}
		return rs;
	}

	public static byte[] ip2Bytes(String ipString) {
		byte[] ip = new byte[4];
		// 取 ip 的各段
		String[] ipSlices = ipString.split("\\.");
		int rs = 0;
		for (int i = 0; i < ipSlices.length; i++) {
			ip[i] = (byte) Short.parseShort(ipSlices[i]);
		}
		return ip;
	}

	public static String int2Ip(int ipInt) {
		String[] ipString = new String[4];
		for (int i = 0; i < 4; i++) {
			// 每 8 位为一段，这里取当前要处理的最高位的位置
			int pos = i * 8;
			// 取当前处理的 ip 段的值
			int and = ipInt & (255 << pos);
			// 将当前 ip 段转换为 0 ~ 255 的数字，注意这里必须使用无符号右移
			ipString[i] = String.valueOf(and >>> pos);
		}
		return String.join(".", ipString);
	}

	public static String charToInt8bit(char[] c, int start){
		if(c.length < start+7) return "";
		int result = 0;
		for(int i=0, a=128; i<8; i++, a/=2){
			if(c[start+i] == '1'){
				result += a;
			}
		}
		return String.valueOf(result);
	}
	public static String charToInt(char[] c, int start, int length){
		if(c.length < start+length-1) return "";
		long result = 0;
		for(int i=1, a=1; i<=length; i++, a*=2){
			if(c[start+length-i] == '1'){
				result += a;
			}
		}
		return String.valueOf(result);
	}

	public static void printCount(HashMap<Integer, Vector<Integer>> table, TSBDD bdd){
		int i=0;
		for(Integer p: table.keySet()){
			Vector<Integer> count = table.get(p);
			System.out.println(i+"-predicate:"+p);
			System.out.println(i+"-count:" + count.toString());
			i++;
		}
	}

	public static int getRandomString(){
//		String str="ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
//		Random random=new Random();
//		StringBuffer sb=new StringBuffer();
//		for(int i=0;i<8;i++){
//			int number=random.nextInt(36);
//			sb.append(str.charAt(number));
//		}
//		String s = sb.toString();
		return IDCount++;
	}

	public static void resetIDCount(){
		IDCount = 0;
	}

	public static double nanotimeToMillsTime(long time){
		return time/1000000.0;
	}

	public static double nanotimeToMillsTime(double time){
		return time/1000000.0;
	}

	public static void main(String[] args){
	}

}
