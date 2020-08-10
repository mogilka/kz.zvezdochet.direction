package kz.zvezdochet.direction.handler;

import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.e4.core.contexts.Active;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.jfree.data.category.DefaultCategoryDataset;

import com.itextpdf.text.BaseColor;
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

import kz.zvezdochet.bean.AspectType;
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

			List<Long> ydates = new ArrayList<Long>();
			Map<Long, Map<Long, List<Long>>> yhouses = new TreeMap<Long, Map<Long, List<Long>>>();
			Map<Long, Map<String, List<SkyPointAspect>>> dtexts = new TreeMap<Long, Map<String, List<SkyPointAspect>>>();

			System.out.println("Prepared for: " + (System.currentTimeMillis() - run));
			run = System.currentTimeMillis();

			//разбивка дат по годам и месяцам
			for (Date date = start.getTime(); start.before(end); start.add(Calendar.DATE, 1), date = start.getTime()) {
				Calendar calendar = Calendar.getInstance();
				calendar.setTime(date);
				long time = date.getTime(); 
				ydates.add(time);
			}
			Collections.sort(ydates);

			/**
			 * коды ингрессий, используемых в отчёте
			 */
			String[] icodes = Ingress.getKeys();

			//создаём аналогичный массив, но с домами вместо дат
			int k = -1;
			for (Long time : ydates) {
				++k;
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

				Map<String, List<SkyPointAspect>> ingressmap = new TreeMap<String, List<SkyPointAspect>>();
				for (Map.Entry<String, List<Object>> ientry : ingressList.entrySet()) {
					String key = ientry.getKey();
					if (!Arrays.asList(icodes).contains(key))
						continue;

					if (key.contains("REPEAT")
							&& (k > 0
								&& !DateUtil.formatDate(date).equals(DateUtil.formatDate(finalDate))))
						continue;

					List<SkyPointAspect> objects2 = ingressmap.containsKey(key) ? ingressmap.get(key) : new ArrayList<SkyPointAspect>();
					List<Object> ingresses = ientry.getValue();
					for (Object object : ingresses) {
						if (object instanceof Planet)
							continue;
						SkyPointAspect spa = (SkyPointAspect)object;
						Planet skyPoint = (Planet)spa.getSkyPoint1();
						if (skyPoint.getCode().equals("Moon"))
							continue;

						SkyPoint skyPoint2 = spa.getSkyPoint2();
						if (skyPoint2 instanceof House
								&& !spa.getAspect().getCode().equals("CONJUNCTION"))
							continue;

						long pid = skyPoint.getId();
						long hid = skyPoint2.getId();
						if (skyPoint2 instanceof House) {
							Map<Long, List<Long>> dmap = yhouses.containsKey(hid) ? yhouses.get(hid) : new TreeMap<Long, List<Long>>();
							List<Long> pmap = dmap.containsKey(time) ? dmap.get(time) : new ArrayList<Long>();
							pmap.add(pid);
							dmap.put(time, pmap);
							yhouses.put(hid, dmap);
						} else {
							if (skyPoint.isMain())
								continue;
							objects2.add(spa);
						}
					}
					ingressmap.put(key, objects2);
				}
				dtexts.put(time, ingressmap);
			}
			ydates = null;
			System.out.println("Composed for: " + (System.currentTimeMillis() - run));

			//генерируем диаграммы домов
			run = System.currentTimeMillis();
	        sdf = new SimpleDateFormat("dd.MM.yy");
			int i = -1;
			for (Map.Entry<Long, Map<Long, List<Long>>> entry : yhouses.entrySet()) {
				long houseid = entry.getKey();
				House house = houses.get(houseid);

	        	Map<Long, List<Long>> map = entry.getValue();
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
						dataset.addValue(planet.getNumber(), planet.getCode(), sdf.format(new Date(d)));
					}
				}
				if (dataset.getColumnCount() > 0) {
					if (++i > 1) {
						i = 0;
						chapter.add(Chunk.NEXTPAGE);
					}
		        	Section section = PDFUtil.printSection(chapter, house.getName(), null);
					Image image = PDFUtil.printLineChart(writer, "", "", "Баллы", dataset, 500, 0, true);
					section.add(image);
				}
			}
			yhouses = null;
			doc.add(chapter);
			doc.add(Chunk.NEXTPAGE);

			//генерируем транзиты планет
			chapter = new ChapterAutoNumber("Аспекты медленных планет");
			chapter.setNumberDepth(0);
			p = new Paragraph("Аспекты медленных планет", new Font(baseFont, 16, Font.BOLD, PDFUtil.FONTCOLORH));
			p.setAlignment(Element.ALIGN_CENTER);
			chapter.add(p);

			Font grayfont = PDFUtil.getAnnotationFont(false);
			sdf = new SimpleDateFormat("EEEE, d MMMM yyyy");

			for (Map.Entry<Long, Map<String, List<SkyPointAspect>>> dentry : dtexts.entrySet()) {
				Map<String, List<SkyPointAspect>> imap = dentry.getValue();
				boolean empty = true;
				for (Map.Entry<String, List<SkyPointAspect>> daytexts : imap.entrySet()) {
					List<SkyPointAspect> ingresses = daytexts.getValue();
					if (!ingresses.isEmpty()) {
						empty = false;
						break;
					}
				}
				if (empty) continue;

				String ym =  sdf.format(new Date(dentry.getKey()));
				Section daysection = PDFUtil.printSection(chapter, ym, null);

				for (Map.Entry<String, List<SkyPointAspect>> itexts : imap.entrySet()) {
					List<SkyPointAspect> ingresses = itexts.getValue();
					for (Object object : ingresses) {
						text = "";

						if (object instanceof SkyPointAspect) {
							SkyPointAspect spa = (SkyPointAspect)object;
							Planet planet = (Planet)spa.getSkyPoint1();
    		                
    		                boolean exact = itexts.getKey().contains("EXACT");
    		                boolean separation = itexts.getKey().contains("SEPARATION");
    		                boolean repeat = itexts.getKey().contains("REPEAT");

							SkyPoint skyPoint = spa.getSkyPoint2();
							String acode = spa.getAspect().getCode();
	    		            String rduration = spa.isRetro() ? " и более" : "";

							String prefix = "";
							if (exact)
								prefix = "Начинается: ";
							else if (repeat)
								prefix = "Продолжается: ";
							else if (separation)
								prefix = "Заканчивается: ";

							AspectType type = spa.getAspect().getType();
							String typeColor = type.getFontColor();
							BaseColor color = PDFUtil.htmlColor2Base(typeColor);
							Font colorbold = new Font(baseFont, 12, Font.BOLD, color);
		    				String tduration = spa.getTransitDuration();

							if (skyPoint instanceof Planet) {
								Planet planet2 = (Planet)skyPoint;
								String ptext = prefix + planet.getShortName() + " " + type.getSymbol() + " " + planet2.getShortName();
								daysection.addSection(new Paragraph(ptext, colorbold));
			    				if (tduration.length() > 0)
				    				daysection.add(new Paragraph("Длительность прогноза: " + tduration + rduration, grayfont));
							}

							if (spa.isRetro()
									&& !separation
									&& !planet.isFictitious()
									&& !type.getCode().contains("POSITIVE")) {
								String str = "Т.к. в этот период " + planet.getName() + " находится в ретро-фазе, то длительность прогноза затянется, а описанные события ";
								if (acode.equals("CONJUNCTION"))
									str += "приобретут для вас особую важность и в будущем ещё напомнят о себе";
								else
									str += "будут носить необратимый характер";
								daysection.add(new Paragraph(str, grayfont));
							}
						}
						daysection.add(Chunk.NEWLINE);
					}
				}
			}
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
