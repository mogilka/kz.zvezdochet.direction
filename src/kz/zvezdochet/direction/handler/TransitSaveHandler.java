package kz.zvezdochet.direction.handler;

import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.e4.core.contexts.Active;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeriesDataItem;

import com.itextpdf.text.Chapter;
import com.itextpdf.text.ChapterAutoNumber;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.ListItem;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Section;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfWriter;

import kz.zvezdochet.bean.Aspect;
import kz.zvezdochet.bean.AspectType;
import kz.zvezdochet.bean.Event;
import kz.zvezdochet.bean.House;
import kz.zvezdochet.bean.Place;
import kz.zvezdochet.bean.Planet;
import kz.zvezdochet.bean.SkyPoint;
import kz.zvezdochet.bean.SkyPointAspect;
import kz.zvezdochet.core.bean.Model;
import kz.zvezdochet.core.handler.Handler;
import kz.zvezdochet.core.util.CalcUtil;
import kz.zvezdochet.core.util.DateUtil;
import kz.zvezdochet.core.util.PlatformUtil;
import kz.zvezdochet.direction.Activator;
import kz.zvezdochet.direction.bean.PeriodItem;
import kz.zvezdochet.direction.part.TransitPart;
import kz.zvezdochet.direction.service.DirectionAspectService;
import kz.zvezdochet.direction.service.DirectionService;
import kz.zvezdochet.export.bean.Bar;
import kz.zvezdochet.export.handler.PageEventHandler;
import kz.zvezdochet.export.util.PDFUtil;
import kz.zvezdochet.service.AspectService;

/**
 * Генерация прогноза за указанный период по месяцам
 * @author Natalie Didenko
 */
public class TransitSaveHandler extends Handler {
	private BaseFont baseFont;

	public TransitSaveHandler() {
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

			List<Model> aspects = new AspectService().getMajorList();

			String filename = PlatformUtil.getPath(Activator.PLUGIN_ID, "/out/transits.pdf").getPath();
			PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(filename));
	        writer.setPageEvent(new PageEventHandler());
	        doc.open();

	    	Font font = PDFUtil.getRegularFont();

	        //metadata
	        PDFUtil.getMetaData(doc, "Прогноз по месяцам");

	        //раздел
			Chapter chapter = new ChapterAutoNumber("Прогноз по месяцам");
			chapter.setNumberDepth(0);

			//шапка
			Paragraph p = new Paragraph();
			PDFUtil.printHeader(p, "Прогноз по месяцам", null);
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

			chapter.add(new Paragraph("Данный прогноз сделан с учётом вашего текущего местонахождения. "
				+ "Если в течение прогнозного периода вы переедете в более отдалённое место (в другой часовой пояс или с ощутимой сменой географической широты), "
				+ "то в некоторых аспектах прогноз будет иметь временны́е погрешности.", font));
			chapter.add(Chunk.NEWLINE);

			Font bold = new Font(baseFont, 12, Font.BOLD);
			chapter.add(new Paragraph("Диаграммы показывают динамику событий по месяцам в трёх категориях: позитив, негатив и важное.", font));
			chapter.add(new Paragraph("Позитив и негатив:", bold));

			com.itextpdf.text.List list = new com.itextpdf.text.List(false, false, 10);
			list = new com.itextpdf.text.List(false, false, 10);
			ListItem li = new ListItem();
	        li.add(new Chunk("«Позитив» – это благоприятные возможности, которые нужно использовать по максимуму.", new Font(baseFont, 12, Font.NORMAL, PDFUtil.FONTGREEN)));
	        list.add(li);

			li = new ListItem();
	        li.add(new Chunk("«Негатив» – это напряжение и отсутствие удачи, к которым надо быть готовым. Выработайте тактику решения проблемы и не предпринимайте рисковых действий.", new Font(baseFont, 12, Font.NORMAL, PDFUtil.FONTCOLORED)));
	        list.add(li);

			li = new ListItem();
	        li.add(new Chunk("Особого внимания заслуживают ломаные графики, – они указывают на значимые для вас события, особенно в сочетании с «Важным».", font));
	        list.add(li);
	        chapter.add(list);
			chapter.add(Chunk.NEWLINE);
			
			chapter.add(new Paragraph("Важное:", bold));
			list = new com.itextpdf.text.List(false, false, 10);
			li = new ListItem();
	        li.add(new Chunk("«Важное» – сильнее всего влияет на ваше поведение в указанный период, особенно в сочетании с «Позитивом» или «Негативом».", font));
	        list.add(li);

			li = new ListItem();
	        li.add(new Chunk("Если рядом с «Важным» отсутствует «Позитив», значит решения нужно принимать обдуманно, "
	        	+ "т.к. они окажутся значимыми для вас и ваших близких и могут иметь непредвиденные последствия", new Font(baseFont, 12, Font.NORMAL, PDFUtil.FONTCOLORED)));
	        list.add(li);
	        chapter.add(list);
			chapter.add(Chunk.NEWLINE);

			chapter.add(new Paragraph("Погрешность прогноза составляет ±1 день.", bold));

			list = new com.itextpdf.text.List(false, false, 10);
			li = new ListItem();
	        li.add(new Chunk("Если график представляет собой точку, значит актуальность данной сферы жизни будет ограничена одним днём.", font));
	        list.add(li);

			li = new ListItem();
	        li.add(new Chunk("Если график изображён в виде линии, то точки на нём укажут на важные дни периода в данной сфере.", font));
	        list.add(li);
	        chapter.add(list);
	        doc.add(chapter);

			Map<Integer, Map<Integer, List<Long>>> years = new TreeMap<Integer, Map<Integer, List<Long>>>();
			Map<Integer, Map<Integer, Map<Long, Map<Long, List<TimeSeriesDataItem>>>>> hyears = new TreeMap<Integer, Map<Integer, Map<Long, Map<Long, List<TimeSeriesDataItem>>>>>();
			Map<Integer, Map<Integer, Map<Long, List<SkyPointAspect>>>> texts = new TreeMap<Integer, Map<Integer, Map<Long, List<SkyPointAspect>>>>();

			System.out.println("Prepared for: " + (System.currentTimeMillis() - run));
			run = System.currentTimeMillis();

			//разбивка дат по годам и месяцам
			for (Date date = start.getTime(); start.before(end); start.add(Calendar.DATE, 1), date = start.getTime()) {
				Calendar calendar = Calendar.getInstance();
				calendar.setTime(date);
				int y = calendar.get(Calendar.YEAR);
				int m = calendar.get(Calendar.MONTH);
				
				Map<Integer, List<Long>> months = years.containsKey(y) ? years.get(y) : new TreeMap<Integer, List<Long>>();
				List<Long> dates = months.containsKey(m) ? months.get(m) : new ArrayList<Long>();
				long time = date.getTime(); 
				if (!dates.contains(time))
					dates.add(time);
				Collections.sort(dates);
				months.put(m, dates);
				years.put(y, months);

				Map<Integer, Map<Long, Map<Long, List<TimeSeriesDataItem>>>> months2 = hyears.containsKey(y) ? hyears.get(y) : new TreeMap<Integer, Map<Long, Map<Long, List<TimeSeriesDataItem>>>>();
				months2.put(m, new TreeMap<Long, Map<Long, List<TimeSeriesDataItem>>>());
				hyears.put(y, months2);
			}

			Map<Integer, Map<String, Map<Integer,Integer>>> yitems = new TreeMap<Integer, Map<String,Map<Integer,Integer>>>();

			//создаём аналогичный массив, но с домами вместо дат
			for (Map.Entry<Integer, Map<Integer, List<Long>>> entry : years.entrySet()) {
				int y = entry.getKey();
				Map<Integer, List<Long>> months = years.get(y);
				Map<Integer, Map<Long, List<SkyPointAspect>>> mtexts = texts.get(y);
				if (null == mtexts)
					mtexts = new TreeMap<Integer, Map<Long,List<SkyPointAspect>>>();

				//данные для графика года
				Map<Integer,Integer> positive = new HashMap<Integer,Integer>();
				Map<Integer,Integer> negative = new HashMap<Integer,Integer>();

				for (int i = 1; i <= 12; i++) {
					positive.put(i, 0);
					negative.put(i, 0);
				}

				//считаем транзиты
				for (Map.Entry<Integer, List<Long>> entry2 : months.entrySet()) {
					int m = entry2.getKey();
					int month = m + 1;

					List<Long> dates = months.get(m);
					Map<Long, List<SkyPointAspect>> dtexts = mtexts.get(m);
					if (null == dtexts)
						dtexts = new TreeMap<Long,List<SkyPointAspect>>();

					for (Long time : dates) {
						Date date = new Date(time);
						String sdate = DateUtil.formatCustomDateTime(date, "yyyy-MM-dd") + " 12:00:00";
						Event event = new Event();
						Date edate = DateUtil.getDatabaseDateTime(sdate);
						event.setBirth(edate);
						event.setPlace(place);
						event.setZone(zone);
						event.calc(true);
						event.initAspects();

						List<SkyPointAspect> spas = dtexts.get(time);
						if (null == spas)
							spas = new ArrayList<SkyPointAspect>();

						Map<Long, Map<Long, Integer>> hitems = new HashMap<Long, Map<Long, Integer>>();

						Collection<Planet> eplanets = event.getPlanets().values();
						for (Planet eplanet : eplanets) {
							for (Model model : planets.values()) {
								Planet planet = (Planet)model;
								PeriodItem item = calc(eplanet, planet, aspects);
								if (null == item)
									continue;
								spas.add(item.getPlanetAspect());

								String code = item.aspect.getType().getCode();
								if (code.equals("NEUTRAL")) {
									if (eplanet.getCode().equals("Lilith") || eplanet.getCode().equals("Kethu")
											|| planet.getCode().equals("Lilith") || planet.getCode().equals("Kethu"))
										negative.put(month, negative.get(month) + 1);
									else
										positive.put(month, positive.get(month) + 1);
								} else if (code.equals("POSITIVE"))
									positive.put(month, positive.get(month) + 1);
								else if (code.equals("NEGATIVE"))
									negative.put(month, negative.get(month) + 1);
							}

							for (Model model : houses.values()) {
								House house = (House)model;
								PeriodItem item = calc(eplanet, house, aspects);
								if (null == item)
									continue;
								spas.add(item.getHouseAspect());

								//данные для диаграммы домов
								long id = house.getId();
								Map<Long, Integer> amap = hitems.containsKey(id) ? hitems.get(id) : new HashMap<Long, Integer>();
								String code = item.aspect.getType().getCode();
								long aid = code.equals("NEUTRAL")
										&& (eplanet.getCode().equals("Lilith") || eplanet.getCode().equals("Kethu"))
									? 2 : item.aspect.getTypeid();
								int val = amap.containsKey(aid) ? amap.get(aid) : 0;
								amap.put(aid, val + 1);
								hitems.put(id, amap);

								if (code.equals("NEUTRAL")) {
									if (eplanet.getCode().equals("Lilith") || eplanet.getCode().equals("Kethu"))
										negative.put(month, negative.get(month) + 1);
									else
										positive.put(month, positive.get(month) + 1);
								} else if (code.equals("POSITIVE"))
									positive.put(month, positive.get(month) + 1);
								else if (code.equals("NEGATIVE"))
									negative.put(month, negative.get(month) + 1);							
							}
						}
						dtexts.put(time, spas);
						mtexts.put(m, dtexts);
						texts.put(y, mtexts);

						Map<Integer, Map<Long, Map<Long, List<TimeSeriesDataItem>>>> months2 = hyears.containsKey(y) ? hyears.get(y) : new TreeMap<Integer, Map<Long, Map<Long, List<TimeSeriesDataItem>>>>();
						Map<Long, Map<Long, List<TimeSeriesDataItem>>> items = months2.containsKey(m) ? months2.get(m) : new TreeMap<Long, Map<Long, List<TimeSeriesDataItem>>>();
						for (Map.Entry<Long, Map<Long, Integer>> entryh : hitems.entrySet()) {
							Map<Long, Integer> amap = entryh.getValue();
							long hid = entryh.getKey();
							Map<Long, List<TimeSeriesDataItem>> map = items.containsKey(hid) ? items.get(hid) : new HashMap<Long, List<TimeSeriesDataItem>>();
							for (Map.Entry<Long, Integer> entry3 : amap.entrySet()) {
								long aid = entry3.getKey();
								List<TimeSeriesDataItem> series = map.containsKey(aid) ? map.get(aid) : new ArrayList<TimeSeriesDataItem>();
								TimeSeriesDataItem tsdi = new TimeSeriesDataItem(new Day(date), entry3.getValue());
								if (!series.contains(tsdi))
									series.add(tsdi);
								map.put(aid, series);
								items.put(hid, map);
							}
						}
						months2.put(m, items);
					}
				}
				Map<String, Map<Integer,Integer>> ymap = new HashMap<String, Map<Integer,Integer>>();
				ymap.put("positive", positive);
				ymap.put("negative", negative);
				yitems.put(y, ymap);
			}
			years = null;
			System.out.println("Composed for: " + (System.currentTimeMillis() - run));

			//генерируем документ
			run = System.currentTimeMillis();
	        Font hfont = new Font(baseFont, 16, Font.BOLD, PDFUtil.FONTCOLOR);

			DirectionService service = new DirectionService();
			DirectionAspectService servicea = new DirectionAspectService();

	        //года
			for (Map.Entry<Integer, Map<Integer, Map<Long, Map<Long, List<TimeSeriesDataItem>>>>> entry : hyears.entrySet()) {
				int y = entry.getKey();
				String syear = String.valueOf(y);
				chapter = new ChapterAutoNumber(PDFUtil.printHeader(new Paragraph(), syear, null));
				chapter.setNumberDepth(0);

				//диаграмма года
				Section section = PDFUtil.printSection(chapter, "Соотношение событий года", null);
				Map<String, Map<Integer,Integer>> ymap = yitems.get(y);
				Map<Integer,Integer> positive = ymap.get("positive");
				Map<Integer,Integer> negative = ymap.get("negative");

				Bar[] bars = new Bar[24];
				for (int i = 0; i < 12; i++) {
					int month = i + 1;
					String smonth = DateUtil.getMonthName(month);
					bars[i] = new Bar(smonth, positive.get(month), null, "Позитивные события", null);
					bars[i + 12] = new Bar(smonth, negative.get(month) * (-1), null, "Негативные события", null);
				}
				Image image = PDFUtil.printStackChart(writer, "Месяцы года", "Возраст", "Количество", bars, 500, 400, true);
				section.add(image);
				section.add(Chunk.NEXTPAGE);

				//месяцы
				Map<Integer, Map<Long, Map<Long, List<TimeSeriesDataItem>>>> months2 = hyears.get(y);
				Map<Integer, Map<Long, List<SkyPointAspect>>> mtexts = texts.get(y);
				for (Map.Entry<Integer, Map<Long, Map<Long, List<TimeSeriesDataItem>>>> entry2 : months2.entrySet()) {
					int m = entry2.getKey();
					Calendar calendar = Calendar.getInstance();
					calendar.set(y, m, 1);
					String ym = new SimpleDateFormat("LLLL").format(calendar.getTime()) + " " + y;
					section = PDFUtil.printSection(chapter, ym, null);

					//TODO диаграмма месяца перед толкованиями
					
					Map<Long, List<SkyPointAspect>> dtexts = mtexts.get(m);
					for (Map.Entry<Long, List<SkyPointAspect>> dentry : dtexts.entrySet()) {
						String shortdate = sdf.format(new Date(dentry.getKey()));
						Section dsection = section.addSection(new Paragraph(shortdate, hfont));

			        	List<SkyPointAspect> spas = dtexts.get(dentry.getKey());
						for (SkyPointAspect spa : spas) {
							AspectType type = spa.getAspect().getType();
							Planet planet = (Planet)spa.getSkyPoint1();

							SkyPoint skyPoint = spa.getSkyPoint2();
							if (skyPoint instanceof House) {
								House house = (House)skyPoint;
//								DirectionText dirText = (DirectionText)service.find(planet, house, type);
								boolean bad = type.getPoints() < 0;
			    				String pname = bad ? planet.getNegative() : planet.getPositive();
								dsection.addSection(new Paragraph(house.getName() + " " + type.getSymbol() + " " + pname, font));
//								if (dirText != null) {
//									String text = dirText.getText();
//									if (text != null) {
//										String typeColor = type.getFontColor();
//										BaseColor color = PDFUtil.htmlColor2Base(typeColor);
//										section.add(new Paragraph(PDFUtil.removeTags(text, new Font(baseFont, 12, Font.NORMAL, color))));
//										PDFUtil.printGender(section, dirText, female, child, true);
//									}
//								}
							} else if (skyPoint instanceof Planet) {
								Planet planet2 = (Planet)skyPoint;
//								PlanetAspectText dirText = (PlanetAspectText)servicea.find(spa, false);
			    				dsection.addSection(new Paragraph(planet.getShortName() + " " + type.getSymbol() + " " + planet2.getShortName(), font));
//									if (dirText != null) {
//										String text = dirText.getText();
//										if (null == text)
//											continue;
//					    				section.addSection(new Paragraph(planet.getShortName() + " " + type.getSymbol() + " " + planet2.getShortName(), fonth5));
//										if (dirText != null) {
//											String text = dirText.getText();
//											if (text != null) {
//								    			String typeColor = type.getFontColor();
//												BaseColor color = PDFUtil.htmlColor2Base(typeColor);
//												section.add(new Paragraph(PDFUtil.removeTags(text, new Font(baseFont, 12, Font.NORMAL, color))));
//												PDFUtil.printGender(section, dirText, female, child, true);
//											}
//										}
//									}
							}
						}
						dsection.add(Chunk.NEWLINE);
					}

					//графики по домам
//					section = PDFUtil.printSection(chapter, ym + " по сферам жизни", null);
//					Map<Long, Map<Long, List<TimeSeriesDataItem>>> items = entry2.getValue();
//			        int i = -1;
//					for (Map.Entry<Long, Map<Long, List<TimeSeriesDataItem>>> entryh : items.entrySet()) {
//						long houseid = entryh.getKey();
//						House house = houses.get(houseid);
//						Map<Long, List<TimeSeriesDataItem>> map = entryh.getValue();
//						TimeSeriesCollection dataset = new TimeSeriesCollection();
//						for (Map.Entry<Long, List<TimeSeriesDataItem>> entry3 : map.entrySet()) {
//			        		List<TimeSeriesDataItem> series = entry3.getValue();
//			        		if (null == series || 0 == series.size())
//			        			continue;
//			        		Long aid = entry3.getKey();
//							TimeSeries timeSeries = new TimeSeries(aid < 2 ? "Важное" : (aid < 3 ? "Негатив" : "Позитив"));
//							for (TimeSeriesDataItem tsdi : series)
//								timeSeries.add(tsdi);
//							dataset.addSeries(timeSeries);
//			        	}
//			        	if (dataset.getSeriesCount() > 0) {
//			        		if (++i > 1) {
//			        			i = 0;
//			        			section.add(Chunk.NEXTPAGE);
//			        		}
//				        	section.addSection(new Paragraph(house.getName(), hfont));
//				        	section.add(new Paragraph(ym + ": " + house.getDescription(), font));
//						    image = PDFUtil.printTimeChart(writer, "", "", "Баллы", dataset, 500, 0, true);
//							section.add(image);
//							section.add(Chunk.NEWLINE);
//			        	}
//					}
			        chapter.add(Chunk.NEXTPAGE);
				}
				doc.add(chapter);
			}
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

	/**
	 * Определение аспектной дирекции между небесными точками
	 * @param point1 первая небесная точка
	 * @param point2 вторая небесная точка
	 * @param aspects список аспектов
	 */
	private PeriodItem calc(SkyPoint point1, SkyPoint point2, List<Model> aspects) {
		try {
			//находим угол между точками космограммы
			double one = point1.getLongitude();
			double two = point2.getLongitude();
			double res = CalcUtil.getDifference(one, two);

			//искусственно устанавливаем нарастающую оппозицию,
			//чтобы она синхронизировалась с соответствующим ей соединением в этот день
			if (point2 instanceof House)
				if ((res >= 179 && res < 180)
						|| CalcUtil.compareAngles(one, two))
					++res;

			//определяем, является ли аспект стандартным
			for (Model realasp : aspects) {
				Aspect a = (Aspect)realasp;

				//соединения Солнца не рассматриваем
				if (a.getPlanetid() > 0)
					continue;

				if (a.isExact(res)) {
					PeriodItem item = new PeriodItem();
					item.aspect = a;
					item.planet = (Planet)point1;

					if (point2 instanceof House)
						item.house = (House)point2;
					else if (point2 instanceof Planet) {
						Planet planet2 = (Planet)point2;
						item.planet2 = planet2;
						item.house = planet2.getHouse();
					}
//					System.out.println(point1.getName() + " " + type.getSymbol() + " " + point2.getName());
					return item;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
