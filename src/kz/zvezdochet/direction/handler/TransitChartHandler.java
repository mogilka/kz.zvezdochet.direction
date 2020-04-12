package kz.zvezdochet.direction.handler;

import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.e4.core.contexts.Active;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.jfree.data.category.DefaultCategoryDataset;

import com.itextpdf.text.Chapter;
import com.itextpdf.text.ChapterAutoNumber;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Section;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfWriter;

import kz.zvezdochet.bean.Event;
import kz.zvezdochet.bean.House;
import kz.zvezdochet.bean.Ingress;
import kz.zvezdochet.bean.Place;
import kz.zvezdochet.bean.Planet;
import kz.zvezdochet.bean.SkyPoint;
import kz.zvezdochet.bean.SkyPointAspect;
import kz.zvezdochet.core.handler.Handler;
import kz.zvezdochet.core.util.DateUtil;
import kz.zvezdochet.core.util.PlatformUtil;
import kz.zvezdochet.direction.Activator;
import kz.zvezdochet.direction.part.TransitPart;
import kz.zvezdochet.export.handler.PageEventHandler;
import kz.zvezdochet.export.util.PDFUtil;

/**
 * Генерация графиков прогноза за указанный период по сферам жизни
 * @author Natalie Didenko
 */
public class TransitChartHandler extends Handler {
	private BaseFont baseFont;

	public TransitChartHandler() {
		super();
		try {
			baseFont = PDFUtil.getBaseFont();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Execute
	public void execute(@Active MPart activePart) {
		Document doc = new Document();
		try {
			long duration = System.currentTimeMillis();
			long run = duration;
			TransitPart periodPart = (TransitPart)activePart.getObject();
			if (!periodPart.check(0)) return;

			Event person = periodPart.getPerson();
			Place place = periodPart.getPlace();
			double zone = periodPart.getZone();
			Map<Long, House> houses = person.getHouses();
			Map<Long, Planet> planets = person.getPlanets();
			updateStatus("Расчёт транзитов на период", false);

			Date initDate = periodPart.getInitialDate();
			Date finalDate = periodPart.getFinalDate();
			Calendar start = Calendar.getInstance();
			start.setTime(initDate);

			Calendar end = Calendar.getInstance();
			end.setTime(finalDate);

			String filename = PlatformUtil.getPath(Activator.PLUGIN_ID, "/out/charts.pdf").getPath();
			PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(filename));
	        writer.setPageEvent(new PageEventHandler());
	        doc.open();

	    	Font font = PDFUtil.getRegularFont();

	        //metadata
	        PDFUtil.getMetaData(doc, "Транзиты по домам");

	        //раздел
			Chapter chapter = new ChapterAutoNumber("Транзиты по домам");
			chapter.setNumberDepth(0);

			//шапка
			Paragraph p = new Paragraph();
			PDFUtil.printHeader(p, "Транзиты по домам", null);
			chapter.add(p);

			String text = person.getCallname() + ": ";
			SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d MMMM yyyy");
			text += sdf.format(initDate);
			boolean days = (DateUtil.getDateFromDate(initDate) != DateUtil.getDateFromDate(finalDate)
				|| DateUtil.getMonthFromDate(initDate) != DateUtil.getMonthFromDate(finalDate)
				|| DateUtil.getYearFromDate(initDate) != DateUtil.getYearFromDate(finalDate));
			if (days)
				text += " — " + sdf.format(finalDate);
			p = new Paragraph(text, font);
	        p.setAlignment(Element.ALIGN_CENTER);
			chapter.add(p);

			if (null == place)
				place = new Place().getDefault();
			text = (zone >= 0 ? "UTC+" : "") + zone +
				" " + place.getName() +
				" " + place.getLatitude() + "°" +
				", " + place.getLongitude() + "°";
			p = new Paragraph(text, font);
	        p.setAlignment(Element.ALIGN_CENTER);
			chapter.add(p);

			Font fontgray = PDFUtil.getAnnotationFont(false);
			text = "Дата составления: " + DateUtil.fulldtf.format(new Date());
			p = new Paragraph(text, fontgray);
	        p.setAlignment(Element.ALIGN_CENTER);
			chapter.add(p);

			p = new Paragraph();
	        p.setAlignment(Element.ALIGN_CENTER);
			p.setSpacingAfter(20);
	        p.add(new Chunk("Автор: ", fontgray));
	        Chunk chunk = new Chunk(PDFUtil.AUTHOR, new Font(baseFont, 10, Font.UNDERLINE, PDFUtil.FONTCOLOR));
	        chunk.setAnchor(PDFUtil.WEBSITE);
	        p.add(chunk);
	        chapter.add(p);

			Map<Integer, List<Long>> years = new TreeMap<Integer, List<Long>>();
			Map<Integer, Map<Long, Map<Long, List<Long>>>> hyears = new TreeMap<Integer, Map<Long, Map<Long, List<Long>>>>();

			System.out.println("Prepared for: " + (System.currentTimeMillis() - run));
			run = System.currentTimeMillis();

			//разбивка дат по годам и месяцам
			for (Date date = start.getTime(); start.before(end); start.add(Calendar.DATE, 1), date = start.getTime()) {
				Calendar calendar = Calendar.getInstance();
				calendar.setTime(date);
				int y = calendar.get(Calendar.YEAR);
				
				List<Long> dates = years.containsKey(y) ? years.get(y) : new ArrayList<Long>();
				long time = date.getTime(); 
				if (!dates.contains(time))
					dates.add(time);
				Collections.sort(dates);
				years.put(y, dates);
			}

			/**
			 * коды ингрессий, используемых в отчёте
			 */
			String[] icodes = new String[] { Ingress._EXACT_HOUSE, Ingress._REPEAT_HOUSE, Ingress._SEPARATION_HOUSE };

			//создаём аналогичный массив, но с домами вместо дат
			for (Map.Entry<Integer, List<Long>> entry : years.entrySet()) {
				int y = entry.getKey();
				List<Long> dates = years.get(y);

				//считаем транзиты
				for (Long time : dates) {
					Date date = new Date(time);
					String sdate = DateUtil.formatCustomDateTime(date, "yyyy-MM-dd") + " 12:00:00";
					Event event = new Event();
					Date edate = DateUtil.getDatabaseDateTime(sdate);
					event.setBirth(edate);
					event.setPlace(place);
					event.setZone(zone);
					event.calc(true);

					Map<String, List<Object>> ingressList = person.initIngresses(event);
					if (ingressList.isEmpty())
						continue;

					Map<Long, Map<Long, List<Long>>> items = hyears.containsKey(y) ? hyears.get(y) : new HashMap<Long, Map<Long, List<Long>>>();

					for (Map.Entry<String, List<Object>> ientry : ingressList.entrySet()) {
						if (!Arrays.asList(icodes).contains(ientry.getKey()))
							continue;
						List<Object> ingresses = ientry.getValue();
						for (Object object : ingresses) {
							SkyPointAspect spa = (SkyPointAspect)object;
							if (!spa.getAspect().getCode().equals("CONJUNCTION"))
								continue;

							SkyPoint skyPoint = spa.getSkyPoint1();
							if (skyPoint.getCode().equals("Moon"))
								continue;

							SkyPoint skyPoint2 = spa.getSkyPoint2();
							if (skyPoint2 instanceof Planet)
								continue;

							long pid = skyPoint.getId();
							long hid = skyPoint2.getId();
							Map<Long, List<Long>> dmap = items.containsKey(hid) ? items.get(hid) : new TreeMap<Long, List<Long>>();
							List<Long> pmap = dmap.containsKey(time) ? dmap.get(time) :new ArrayList<Long>();
							pmap.add(pid);
							dmap.put(time, pmap);
							items.put(hid, dmap);
						}
					}
					hyears.put(y, items);
				}
			}
			years = null;
			System.out.println("Composed for: " + (System.currentTimeMillis() - run));

			//генерируем документ
			run = System.currentTimeMillis();
	        Font hfont = PDFUtil.getHeaderFont();

	        //года
	        sdf = new SimpleDateFormat("dd.MM.yy");
			for (Map.Entry<Integer, Map<Long, Map<Long, List<Long>>>> entry : hyears.entrySet()) {
				int y = entry.getKey();
				String syear = String.valueOf(y);
				Section section = chapter.addSection(new Paragraph(syear + " год", hfont));

				Map<Long, Map<Long, List<Long>>> items = entry.getValue();
				int i = -1;
				for (Map.Entry<Long, Map<Long, List<Long>>> entryh : items.entrySet()) {
					long houseid = entryh.getKey();
					House house = houses.get(houseid);
					Map<Long, List<Long>> map = entryh.getValue();
					if (map.isEmpty())
						continue;
					DefaultCategoryDataset dataset = new DefaultCategoryDataset();
					for (Map.Entry<Long, List<Long>> entry3 : map.entrySet()) {
						List<Long> series = entry3.getValue();
						if (null == series || series.isEmpty())
							continue;
						Long d = entry3.getKey();
						for (int j = 0; j < series.size(); j++) {
							Long pid = series.get(j);
							Planet planet = planets.get(pid);
							dataset.addValue(j + 1, planet.getName(), sdf.format(new Date(d)));
						}
					}
					if (dataset.getColumnCount() > 0) {
						if (++i > 1) {
							i = 0;
							section.add(Chunk.NEXTPAGE);
						}
			        	Section ysection = PDFUtil.printSubsection(section, house.getName() + " " + y, null);
			        	Image image = PDFUtil.printLineChart(writer, "", "", "Баллы", dataset, 500, 0, true);
			        	ysection.add(image);
					}
				}
			}
			hyears = null;
			doc.add(chapter);
			doc.add(Chunk.NEWLINE);
	        doc.add(PDFUtil.printCopyright());

	        long time = System.currentTimeMillis();
			System.out.println("Finished for: " + (time - run));
			System.out.println("Duration: " + (time - duration));
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
	        doc.close();
		}
	}
}
