package edu.psu.ist.acs.micro.event.scratch;

import org.joda.time.LocalDate;
import org.joda.time.Partial;
import org.joda.time.YearMonth;


public class Scratch {
	public static void main(String[] args) {
		YearMonth ym = new YearMonth(1999, 12);
		LocalDate ld = new LocalDate(1999, 12, 10);
		System.out.println(ym.toString());
		Partial p = new Partial(ym);
		System.out.println(p.toString());
		System.out.println(ld.toString());
		System.out.println(YearMonth.parse(ld.toString()).toString());
		System.out.println(LocalDate.parse(ym.toString()).toString());
		
	}
}
