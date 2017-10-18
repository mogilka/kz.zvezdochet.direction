package kz.zvezdochet.direction.handler;

import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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
import com.itextpdf.text.ListItem;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Section;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfWriter;

import kz.zvezdochet.bean.Aspect;
import kz.zvezdochet.bean.Event;
import kz.zvezdochet.bean.House;
import kz.zvezdochet.bean.Place;
import kz.zvezdochet.bean.Planet;
import kz.zvezdochet.bean.SkyPoint;
import kz.zvezdochet.core.bean.Model;
import kz.zvezdochet.core.handler.Handler;
import kz.zvezdochet.core.util.CalcUtil;
import kz.zvezdochet.core.util.DateUtil;
import kz.zvezdochet.core.util.PlatformUtil;
import kz.zvezdochet.direction.Activator;
import kz.zvezdochet.direction.bean.PeriodItem;
import kz.zvezdochet.direction.part.PeriodPart;
import kz.zvezdochet.export.handler.PageEventHandler;
import kz.zvezdochet.export.util.PDFUtil;
import kz.zvezdochet.service.AspectService;
import kz.zvezdochet.service.HouseService;
import kz.zvezdochet.util.Configuration;
import kz.zvezdochet.util.HouseMap;
/**
 * Генерация отчёта за указанный период по месяцам
 * @author Nataly Didenko
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
			PeriodPart periodPart = (PeriodPart)activePart.getObject();
				if (!periodPart.check(0)) return;
			Event person = periodPart.getPerson();

			Place place = periodPart.getPlace();
			double zone = periodPart.getZone();
	
			Configuration conf = person.getConfiguration();
			List<Model> planets = conf.getPlanets();
			List<Model> houses = conf.getHouses();
	
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
	        writer.setPageEvent(new PageEventHandler(doc));
	        doc.open();

	    	Font font = PDFUtil.getRegularFont();

	        //metadata
	        PDFUtil.getMetaData(doc, "Прогноз событий по месяцам");

	        //раздел
			Chapter chapter = new ChapterAutoNumber("Общая информация");
			chapter.setNumberDepth(1);

			//шапка
			Paragraph p = new Paragraph();
			PDFUtil.printHeader(p, "Прогноз событий по месяцам");
			chapter.add(p);

			SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d MMMM yyyy");
			String text = sdf.format(initDate);
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

			Font fontgray = new Font(baseFont, 10, Font.NORMAL, PDFUtil.FONTCOLORGRAY);
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

			chapter.add(new Paragraph("Расчёт сделан с учётом указанного вами места проживания. "
				+ "Однако если вы в течение данного периода переедете в другое место, то с того момента прогноз будет недействителен, "
				+ "т.к. привязка идёт к конкретному местонахождению. В этом случае после переезда можете написать мне, "
				+ "где вы обосновались, и я составлю прогноз на тот же период, но уже на новое место. "
				+ "Такая релокация будет бесплатна в рамках периода, указанного вами в заказе.", font));

			chapter.add(Chunk.NEWLINE);
			chapter.add(new Paragraph("При этом, если ранее вы получали от меня прогноз по годам, индивидуальный гороскоп или гороскоп совместимости, "
				+ "то они будут действовать независимо от вашего местонахождения", font));

			chapter.add(Chunk.NEWLINE);
			chapter.add(new Paragraph("Диаграммы показывают динамику событий в сферах жизни:", font));

			com.itextpdf.text.List list = new com.itextpdf.text.List(false, false, 10);
			ListItem li = new ListItem();
	        li.add(new Chunk("Показатели выше нуля указывают на успех и лёгкость.", font));
	        list.add(li);

			li = new ListItem();
	        li.add(new Chunk("Показатели на нуле указывают на сбалансированность ситуации.", font));
	        list.add(li);

			li = new ListItem();
	        li.add(new Chunk("Показатели ниже нуля указывают на трудности и напряжение.", font));
	        list.add(li);
	        chapter.add(list);
			doc.add(chapter);

			Map<Long, List<TimeSeriesDataItem>> series = new HashMap<Long, List<TimeSeriesDataItem>>();

			Map<Long, Map<Long, Integer>> dates = new TreeMap<Long, Map<Long, Integer>>();
			System.out.println("Prepared for: " + (System.currentTimeMillis() - run));
			run = System.currentTimeMillis();

			for (Date date = start.getTime(); start.before(end); start.add(Calendar.DATE, 1), date = start.getTime()) {
//				Map<Long, Double> pmap = new HashMap<Long, Double>();

				String sdate = DateUtil.formatCustomDateTime(date, "yyyy-MM-dd") + " 12:00:00";
				Event event = new Event();
				Date edate = DateUtil.getDatabaseDateTime(sdate);
				event.setBirth(edate);
				event.setPlace(place);
				event.setZone(zone);
				event.calc(false);
				event.getConfiguration().initPlanetAspects();

				Event prev = new Event();
				Calendar cal = Calendar.getInstance();
				cal.setTime(edate);
				cal.add(Calendar.DATE, -1);
				prev.setBirth(cal.getTime());
				prev.setPlace(place);
				prev.setZone(zone);
				prev.calc(false);
				prev.getConfiguration().initPlanetAspects();

				List<Planet> iplanets = new ArrayList<Planet>();
				List<Model> eplanets = event.getConfiguration().getPlanets();
				for (Model model : eplanets) {
					Planet planet = (Planet)model;
					List<Object> ingresses = planet.isIngressed(prev, event);
					if (ingresses != null && ingresses.size() > 0)
						iplanets.add(planet);
				}

//				Map<Long, Set<PeriodItem>> pitems = new HashMap<Long, Set<PeriodItem>>();
				Map<Long, Integer> hitems = new HashMap<Long, Integer>();
				for (Planet eplanet : iplanets) {
//					for (Model model : planets) { //TODO придумать, как выводить транзиты планет
//						Planet planet = (Planet)model;
//						PeriodItem item = calc(eplanet, planet);
//						if (null == item)
//							continue;
//						long id = item.aspect.getTypeid();
//						Set<PeriodItem> list = pitems.get(id);
//						if (null == list)
//							list = new HashSet<PeriodItem>();
//						list.add(item);
//						pitems.put(id, list);
//
//						double val = pmap.containsKey(id) ? pmap.get(id) : 0;
//						pmap.put(id, val + 1);
//					}
					for (Model model : houses) {
						House house = (House)model;
						PeriodItem item = calc(eplanet, house, aspects);
						if (null == item)
							continue;
						long id = house.getId();
						int val = hitems.containsKey(id) ? hitems.get(id) : 0;
						hitems.put(id, val + item.aspect.getType().getPoints());
					}
				}

				long time = date.getTime();
				Map<Long, Integer> items = dates.containsKey(time) ? dates.get(time) : hitems;
				dates.put(time, items);
			}
			System.out.println("Composed for: " + (System.currentTimeMillis() - run));

			run = System.currentTimeMillis();
			int year = 0;
			int month = -1;
			Section section = null;
			HouseService service = new HouseService();
			for (Map.Entry<Long, Map<Long, Integer>> entry : dates.entrySet()) {
				Date date = new Date(entry.getKey());

				//разбивка по годам
				Calendar calendar = Calendar.getInstance();
				calendar.setTime(date);
				int y = calendar.get(Calendar.YEAR);
				if (year != y) {
					if (year != 0)
						doc.add(chapter);

					String syear = String.valueOf(y);
					chapter = new ChapterAutoNumber(syear);
					chapter.setNumberDepth(0);
		
					p = new Paragraph();
					PDFUtil.printHeader(p, syear);
					chapter.add(p);

					year = y;
				}

				//разбивка по месяцам
				int m = calendar.get(Calendar.MONTH);
				String mtitle = new SimpleDateFormat("LLLL").format(date) + " " + y;
				if (month != m) {
					section = PDFUtil.printSection(chapter, mtitle);
					month = m;
				}

				Map<Long, Integer> items = entry.getValue();
				for (Map.Entry<Long, Integer> entry2 : items.entrySet()) {
					List<TimeSeriesDataItem> sitems = series.containsKey(entry2.getKey()) ? series.get(entry2.getKey()) : new ArrayList<TimeSeriesDataItem>();
					TimeSeriesDataItem tsdi = new TimeSeriesDataItem(new Day(date), entry2.getValue());
					if (!sitems.contains(tsdi))
						sitems.add(tsdi);
					series.put(entry2.getKey(), sitems);
				}
			}

	        HouseMap[] houseMap = HouseMap.getMap();
	        Font hfont = new Font(baseFont, 16, Font.BOLD, PDFUtil.FONTCOLOR);
	        for (HouseMap hmap : houseMap) {
	        	section.addSection(new Paragraph(hmap.name, hfont));
				List<String> descrs = new ArrayList<String>();
				TimeSeriesCollection dataset = new TimeSeriesCollection();
	        	for (int i = 0; i < 3; i++) {
	        		long houseid = hmap.houseids[i];
	        		List<TimeSeriesDataItem> hdata = series.get(houseid);
	        		if (null == hdata || 0 == hdata.size())
	        			continue;
					House house = (House)service.find(houseid);
					TimeSeries timeSeries = new TimeSeries(house.getName());
					descrs.add(house.getName() + ": " + house.getDescription());
					for (TimeSeriesDataItem tsdi : hdata)
						timeSeries.add(tsdi);
					dataset.addSeries(timeSeries);
	        	}	        	
			    com.itextpdf.text.Image image = PDFUtil.printTimeChart(writer, "", "Даты", "Баллы", dataset, 500, 0, true);
				section.add(image);

				list = new com.itextpdf.text.List(false, false, 10);
				for (String descr : descrs) {
					li = new ListItem();
					li.add(new Chunk(descr, font));
					list.add(li);
				}
				section.add(Chunk.NEWLINE);
				section.add(list);
				section.add(Chunk.NEWLINE);
	        }
			if (start.get(Calendar.YEAR) == end.get(Calendar.YEAR))
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

	/**
	 * Определение аспектной дирекции между небесными точками
	 * @param point1 первая небесная точка
	 * @param point2 вторая небесная точка
	 */
	private PeriodItem calc(SkyPoint point1, SkyPoint point2, List<Model> aspects) {
		try {
			//находим угол между точками космограммы
			double res = CalcUtil.getDifference(point1.getCoord(), point2.getCoord());

			//определяем, является ли аспект стандартным
			for (Model realasp : aspects) {
				Aspect a = (Aspect)realasp;
				if (a.isExact(res)) {
					if (a.getPlanetid() > 0)
						continue;

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
