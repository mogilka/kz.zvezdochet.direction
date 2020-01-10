package kz.zvezdochet.direction.handler;

import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
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

import kz.zvezdochet.analytics.bean.Sphere;
import kz.zvezdochet.analytics.service.SphereService;
import kz.zvezdochet.bean.Aspect;
import kz.zvezdochet.bean.Event;
import kz.zvezdochet.bean.House;
import kz.zvezdochet.bean.Place;
import kz.zvezdochet.bean.Planet;
import kz.zvezdochet.bean.SkyPoint;
import kz.zvezdochet.core.bean.Model;
import kz.zvezdochet.core.handler.Handler;
import kz.zvezdochet.core.ui.util.DialogUtil;
import kz.zvezdochet.core.util.CalcUtil;
import kz.zvezdochet.core.util.DateUtil;
import kz.zvezdochet.core.util.PlatformUtil;
import kz.zvezdochet.direction.Activator;
import kz.zvezdochet.direction.bean.PeriodItem;
import kz.zvezdochet.direction.part.TransitPart;
import kz.zvezdochet.export.bean.Bar;
import kz.zvezdochet.export.handler.PageEventHandler;
import kz.zvezdochet.export.util.PDFUtil;
import kz.zvezdochet.service.AspectService;

/**
 * Генерация прогноза за указанный период по месяцам
 * @author Natalie Didenko
 */
public class MonthHandler extends Handler {
	private BaseFont baseFont;

	public MonthHandler() {
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

			Object[] spheres = periodPart.getSpheres();
			List<Long> selhouses = new ArrayList<>();
			SphereService sphereService = new SphereService();
			for (Object item : spheres) {
				Sphere sphere = (Sphere)item;
				List<Model> houses = sphereService.getHouses(sphere.getId());
				for (Model model : houses)
					if (!selhouses.contains(model.getId()))
						selhouses.add(model.getId());
			}
			if (selhouses.isEmpty()) {
				DialogUtil.alertError("Отметьте галочкой хотя бы одну сферу жизни");
				return;
			}
			List<Long> selplanets = new ArrayList<>();
			for (Object item : spheres) {
				Sphere sphere = (Sphere)item;
				List<Model> planets = sphereService.getPlanets(sphere.getId());
				for (Model model : planets)
					if (!selplanets.contains(model.getId()))
						selplanets.add(model.getId());
			}

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

			String filename = PlatformUtil.getPath(Activator.PLUGIN_ID, "/out/month.pdf").getPath();
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

			Font bold = new Font(baseFont, 12, Font.BOLD);
			chapter.add(new Paragraph("Прогноз на сферы жизни:", bold));
			com.itextpdf.text.List list = new com.itextpdf.text.List(false, false, 10);
			for (Object object : spheres) {
				ListItem li = new ListItem();
				Sphere sphere = (Sphere)object;
				li.add(new Chunk(sphere.getName(), font));
				list.add(li);
			}
		    chapter.add(list);
			chapter.add(Chunk.NEWLINE);

			chapter.add(new Paragraph("Данный прогноз сделан с учётом вашего текущего местонахождения. "
				+ "Если вы в течение прогнозного периода переедете в более отдалённое место (в другой часовой пояс или с ощутимой сменой географической широты), "
				+ "то там прогноз будет недостоверен. Но можно составить аналогичный прогноз на тот же период на новом месте.", font));
			chapter.add(Chunk.NEWLINE);

			chapter.add(new Paragraph("Диаграммы показывают динамику событий по месяцам в трёх категориях: позитив, негатив и важное.", font));
			chapter.add(new Paragraph("Позитив и негатив:", bold));

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

			chapter.add(new Paragraph("Погрешность прогноза составляет ±2 дня.", bold));

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
			Map<Integer, Map<Integer, Map<Long, Map<Long, List<TimeSeriesDataItem>>>>> years2 = new TreeMap<Integer, Map<Integer, Map<Long, Map<Long, List<TimeSeriesDataItem>>>>>();

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

				Map<Integer, Map<Long, Map<Long, List<TimeSeriesDataItem>>>> months2 = years2.containsKey(y) ? years2.get(y) : new TreeMap<Integer, Map<Long, Map<Long, List<TimeSeriesDataItem>>>>();
				months2.put(m, new TreeMap<Long, Map<Long, List<TimeSeriesDataItem>>>());
				years2.put(y, months2);
			}

			Map<Long, List<Date>> revolutions = new HashMap<Long, List<Date>>();
			Map<Integer, Map<String, Map<Integer,Integer>>> yitems = new TreeMap<Integer, Map<String,Map<Integer,Integer>>>();

			//создаём аналогичный массив, но с домами вместо дат
			for (Map.Entry<Integer, Map<Integer, List<Long>>> entry : years.entrySet()) {
				int y = entry.getKey();
				Map<Integer, List<Long>> months = years.get(y);

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
		
						List<Planet> iplanets = new ArrayList<Planet>();
						Collection<Planet> eplanets = event.getPlanets().values();
						for (Model model : eplanets) {
							Planet planet = (Planet)model;
//							List<Object> ingresses = planet.isIngressed(prev, event);
//							if (ingresses != null && ingresses.size() > 0)
								iplanets.add(planet);
						}

						Map<Long, Map<Long, Integer>> hitems = new HashMap<Long, Map<Long, Integer>>();
						for (Planet eplanet : iplanets) {
							for (Model model : houses.values()) {
								if (!selhouses.contains(model.getId()))
									continue;

								House house = (House)model;
								PeriodItem item = calc(eplanet, house, aspects);
								if (null == item)
									continue;
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

							if (!selplanets.contains(eplanet.getId()))
								continue;
							for (Model model : planets.values()) {
								if (!selplanets.contains(model.getId()))
									continue;

								//пока что считаем только революции
								if (eplanet.getId().equals(model.getId())) {
									Planet planet = (Planet)model;
									PeriodItem item = calc(eplanet, planet, aspects);
									if (null == item)
										continue;
									if (!item.aspect.getCode().equals("CONJUNCTION"))
										continue;

									List<Date> dlist = revolutions.get(model.getId());
									if (null == dlist)
										dlist = new ArrayList<Date>();
									dlist.add(edate);
									revolutions.put(model.getId(), dlist);
								}
							}
						}

						Map<Integer, Map<Long, Map<Long, List<TimeSeriesDataItem>>>> months2 = years2.containsKey(y) ? years2.get(y) : new TreeMap<Integer, Map<Long, Map<Long, List<TimeSeriesDataItem>>>>();
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

			//обращения планет
	        System.out.println(revolutions);
	        if (!revolutions.isEmpty()) {
				chapter = new ChapterAutoNumber(PDFUtil.printHeader(new Paragraph(), "Важные циклы", null));
				chapter.setNumberDepth(0);
				chapter.add(new Paragraph("Приведённые ниже даты, связаны с завершением одного большого жизненного цикла и началом другого", font));

				List<String> negative = Arrays.asList(new String[] {"Kethu", "Lilith"});
				for (Map.Entry<Long, List<Date>> entry : revolutions.entrySet()) {
					long planetid = entry.getKey();
					Planet planet = planets.get(planetid);
					Section section = PDFUtil.printSection(chapter, planet.getShortName(), null);

					list = new com.itextpdf.text.List(false, false, 10);
					List<Date> dlist = entry.getValue();
					for (Date date : dlist) {
						li = new ListItem();
				        li.add(new Chunk(sdf.format(date), font));
				        list.add(li);
					}
			        section.add(list);
			        section.add(Chunk.NEWLINE);

					p = new Paragraph();
					p.setFont(font);
					p.add("В указанные дни произойдёт начало нового цикла в сфере «" + planet.getShortName() + "». "
						+ "Это означает, что вас ожидает яркое событие или переживание, связанное со следующими сферами жизни: ");
					p.add(new Chunk(planet.getDescription(), new Font(baseFont, 12, Font.NORMAL, negative.contains(planet.getCode()) ? PDFUtil.FONTCOLORED : PDFUtil.FONTGREEN)));
					section.add(p);
				}
				chapter.add(Chunk.NEXTPAGE);
				doc.add(chapter);
	        }

	        //дома
			for (Map.Entry<Integer, Map<Integer, Map<Long, Map<Long, List<TimeSeriesDataItem>>>>> entry : years2.entrySet()) {
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

				//графики по месяцам
				Map<Integer, Map<Long, Map<Long, List<TimeSeriesDataItem>>>> months2 = years2.get(y);

				for (Map.Entry<Integer, Map<Long, Map<Long, List<TimeSeriesDataItem>>>> entry2 : months2.entrySet()) {
					int m = entry2.getKey();
					Calendar calendar = Calendar.getInstance();
					calendar.set(y, m, 1);
					String ym = new SimpleDateFormat("LLLL").format(calendar.getTime()) + " " + y;
					section = PDFUtil.printSection(chapter, ym, null);

					Map<Long, Map<Long, List<TimeSeriesDataItem>>> items = entry2.getValue();
			        int i = -1;
					for (Map.Entry<Long, Map<Long, List<TimeSeriesDataItem>>> entryh : items.entrySet()) {
						long houseid = entryh.getKey();
						House house = houses.get(houseid);
						Map<Long, List<TimeSeriesDataItem>> map = entryh.getValue();
						TimeSeriesCollection dataset = new TimeSeriesCollection();
						for (Map.Entry<Long, List<TimeSeriesDataItem>> entry3 : map.entrySet()) {
			        		List<TimeSeriesDataItem> series = entry3.getValue();
			        		if (null == series || 0 == series.size())
			        			continue;
			        		Long aid = entry3.getKey();
							TimeSeries timeSeries = new TimeSeries(aid < 2 ? "Важное" : (aid < 3 ? "Негатив" : "Позитив"));
							for (TimeSeriesDataItem tsdi : series)
								timeSeries.add(tsdi);
							dataset.addSeries(timeSeries);
			        	}
			        	if (dataset.getSeriesCount() > 0) {
			        		if (++i > 1) {
			        			i = 0;
			        			section.add(Chunk.NEXTPAGE);
			        		}
				        	section.addSection(new Paragraph(house.getName(), hfont));
				        	section.add(new Paragraph(ym + ": " + house.getDescription(), font));
						    image = PDFUtil.printTimeChart(writer, "", "", "Баллы", dataset, 500, 0, true);
							section.add(image);
							section.add(Chunk.NEWLINE);
			        	}
					}
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

					if (point2 instanceof House)
						item.house = (House)point2;
					else if (point2 instanceof Planet) {
						item.planet = (Planet)point1;
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
