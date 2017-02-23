package kz.zvezdochet.direction.handler;

import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.e4.core.contexts.Active;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;

import com.itextpdf.text.BaseColor;
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
import kz.zvezdochet.bean.AspectType;
import kz.zvezdochet.bean.Event;
import kz.zvezdochet.bean.House;
import kz.zvezdochet.bean.Place;
import kz.zvezdochet.bean.Planet;
import kz.zvezdochet.bean.SkyPoint;
import kz.zvezdochet.core.bean.Model;
import kz.zvezdochet.core.handler.Handler;
import kz.zvezdochet.core.service.DataAccessException;
import kz.zvezdochet.core.util.CalcUtil;
import kz.zvezdochet.core.util.DateUtil;
import kz.zvezdochet.core.util.PlatformUtil;
import kz.zvezdochet.direction.Activator;
import kz.zvezdochet.direction.part.PeriodPart;
import kz.zvezdochet.export.handler.PageEventHandler;
import kz.zvezdochet.export.util.PDFUtil;
import kz.zvezdochet.service.AspectService;
import kz.zvezdochet.service.AspectTypeService;
import kz.zvezdochet.util.Configuration;

import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.TimeSeriesDataItem;
/**
 * Обработчик расчёта транзитов на указанный период
 * @author Nataly Didenko
 */
public class PeriodCalcHandler extends Handler {
	private BaseFont baseFont;

	public PeriodCalcHandler() {
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

			String filename = PlatformUtil.getPath(Activator.PLUGIN_ID, "/out/period.pdf").getPath();
			PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(filename));
	        writer.setPageEvent(new PageEventHandler(doc));
	        doc.open();

	    	Font font = PDFUtil.getRegularFont();

	        //metadata
	        PDFUtil.getMetaData(doc, "Прогноз событий");

	        //раздел
			Chapter chapter = new ChapterAutoNumber("Общая информация");
			chapter.setNumberDepth(0);

			//шапка
			Paragraph p = new Paragraph();
			PDFUtil.printHeader(p, "Прогноз событий");
			chapter.add(p);

			SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d MMMM yyyy");
			String text = sdf.format(initDate);
			if (finalDate.getTime() > initDate.getTime())
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

			Map<Long, List<TimeSeriesDataItem>> series = new HashMap<Long, List<TimeSeriesDataItem>>();

			chapter.add(new Paragraph("Прогноз классифицирует события по 5 признакам:", font));

			AspectTypeService service = new AspectTypeService();
			List<AspectType> types = service.getMainList();
			Font bfont = new Font(baseFont, 12, Font.BOLD, PDFUtil.FONTCOLOR);
			com.itextpdf.text.List alist = new com.itextpdf.text.List(false, false, 10);
			for (AspectType aspectType : types) {
				if (null == aspectType.getDescription())
					continue;
				ListItem li = new ListItem();
		        chunk = new Chunk(aspectType.getDescription(), bfont);
		        li.add(chunk);
		        chunk = new Chunk(" — " + aspectType.getText(), font);
		        li.add(chunk);
		        alist.add(li);
		        series.put(aspectType.getId(), new ArrayList<TimeSeriesDataItem>());
			}
			chapter.add(alist);

			chapter.add(new Paragraph("Если сфера жизни повторно упоминается в течение дня, значит она будет насыщена событиями и мыслями.", font));
			doc.add(chapter);


			for (Date date = start.getTime(); start.before(end); start.add(Calendar.DATE, 1), date = start.getTime()) {
				System.out.println(date);
				String sdfdate = sdf.format(date);
				chapter = new ChapterAutoNumber(sdfdate);
				chapter.setNumberDepth(0);

				p = new Paragraph();
				PDFUtil.printHeader(p, sdfdate);
				chapter.add(p);

				Map<Long, Double> map = new HashMap<Long, Double>();

				for (int i = 1; i < 5; i++) {
					int h = i * 6;
					String shour = (h < 10) ? "0" + h : String.valueOf(h);
					String sdate = DateUtil.formatCustomDateTime(date, "yyyy-MM-dd") + " " + shour + ":00:00";
					System.out.println(shour);

					Event event = new Event();
					Date edate = DateUtil.getDatabaseDateTime(sdate);
					event.setBirth(edate);
					event.setPlace(place);
					event.setZone(zone);
					event.calc(true);

					Event prev = new Event();
					Calendar cal = Calendar.getInstance();
					cal.setTime(edate);
					cal.add(Calendar.DATE, -1);
					prev.setBirth(cal.getTime());
					prev.setPlace(place);
					prev.setZone(zone);
					prev.calc(false);

					List<Planet> iplanets = new ArrayList<Planet>();
					List<Model> eplanets = event.getConfiguration().getPlanets();
					for (Model model : eplanets) {
						Planet planet = (Planet)model;
						List<Object> ingresses = planet.isIngressed(prev, event);
						if (ingresses != null && ingresses.size() > 0)
							iplanets.add(planet);
					}

					Map<Long, List<PeriodItem>> items = new HashMap<Long, List<PeriodItem>>();
					for (Planet eplanet : iplanets) {
						for (Model model : planets) {
							Planet planet = (Planet)model;
							PeriodItem item = calc(eplanet, planet);
							if (null == item)
								continue;
							long id = item.aspectType.getId();
							List<PeriodItem> list = items.get(id);
							if (null == list)
								list = new ArrayList<>();
							list.add(item);
							items.put(id, list);

							double val = map.containsKey(id) ? map.get(id) : 0;
							map.put(id, val + 1);
						}
						for (Model model : houses) {
							House house = (House)model;
							PeriodItem item = calc(eplanet, house);
							if (null == item)
								continue;
							long id = item.aspectType.getId();
							List<PeriodItem> list = items.get(id);
							if (null == list)
								list = new ArrayList<>();
							list.add(item);
							items.put(id, list);

							double val = map.containsKey(id) ? map.get(id) : 0;
							map.put(id, val + 1);
						}
					}

					if (items != null && items.size() > 0) {
						String header = "";
						switch (i) {
							case 2: header = "День"; break;
							case 3: header = "Вечер"; break;
							case 4: header = "Ночь"; break;
							default: header = "Утро"; break;
						}
						Section section = PDFUtil.printSection(chapter, header);
	
						Font fonth5 = PDFUtil.getHeaderFont();
						for (Map.Entry<Long, List<PeriodItem>> entry : items.entrySet()) {
							List<PeriodItem> list = entry.getValue();
							if (null == list || 0 == list.size()) {
								section.add(new Paragraph("Нет данных", font));
								continue;
							}
							AspectType type = (AspectType)service.find(entry.getKey());
							section.add(new Paragraph(type.getDescription(), fonth5));
	
							String typeColor = type.getFontColor();
							BaseColor color = PDFUtil.htmlColor2Base(typeColor);
							alist = new com.itextpdf.text.List(false, false, 10);
							for (PeriodItem item : list) {
								ListItem li = new ListItem();
						        chunk = new Chunk(item.house.getDescription(), new Font(baseFont, 12, Font.NORMAL, color));
						        li.add(chunk);
						        alist.add(li);
							}
							section.add(alist);
						}
					}
				}
				for (Map.Entry<Long, Double> entry : map.entrySet()) {
					List<TimeSeriesDataItem> sitems = series.containsKey(entry.getKey()) ? series.get(entry.getKey()) : new ArrayList<TimeSeriesDataItem>();
					TimeSeriesDataItem tsdi = new TimeSeriesDataItem(new Day(date), entry.getValue());
					if (!sitems.contains(tsdi))
						sitems.add(tsdi);
					series.put(entry.getKey(), sitems);
				}
				doc.add(chapter);
				System.out.println();
			}

			TimeSeriesCollection dataset = new TimeSeriesCollection();
			for (Map.Entry<Long, List<TimeSeriesDataItem>> entry : series.entrySet()) {
				List<TimeSeriesDataItem> sitems = entry.getValue();
				if (null == sitems || 0 == sitems.size())
					continue;
				AspectType asptype = (AspectType)service.find(entry.getKey());
				if (null == asptype.getDescription())
					continue;
				TimeSeries timeSeries = new TimeSeries(asptype.getDescription());
				for (TimeSeriesDataItem tsdi : entry.getValue())
					timeSeries.add(tsdi);
				dataset.addSeries(timeSeries);
			}
			
			doc.add(Chunk.NEWLINE);
		    com.itextpdf.text.Image image = PDFUtil.printTimeChart(writer, "Прогноз периода", "Даты", "Баллы", dataset, 500, 0, true);
			doc.add(image);

			doc.add(Chunk.NEWLINE);
	        doc.add(PDFUtil.printCopyright());
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
	private PeriodItem calc(SkyPoint point1, SkyPoint point2) {
		try {
			//находим угол между точками космограммы
			double res = CalcUtil.getDifference(point1.getCoord(), point2.getCoord());
			House house = null;

			//определяем, является ли аспект стандартным
			List<Model> aspects = new AspectService().getList();
			for (Model realasp : aspects) {
				Aspect a = (Aspect)realasp;
				if (a.isMain() && a.isExactTruncAspect(res)) {
					if (a.getPlanetid() > 0)
						continue;

					if (point2 instanceof House)
						house = (House)point2;
					else if (point2 instanceof Planet) {
						Planet planet2 = (Planet)point2;
						house = planet2.getHouse();
					}
					AspectType type = a.getType();
					PeriodItem item = new PeriodItem();
					item.aspectType = type;
					item.house = house;
					System.out.println(point1.getName() + " " + type.getSymbol() + " " + point2.getName());
					return item;
				}
			}
		} catch (DataAccessException e) {
			e.printStackTrace();
		}
		return null;
	}

	private class PeriodItem {
		public AspectType aspectType;
		public House house;

		 @Override
		 public boolean equals(Object obj) {
			 PeriodItem other = (PeriodItem)obj;
			 return this.house.getId() == other.house.getId() && this.aspectType.getId() == other.aspectType.getId();
		 }

		 @Override
		 public int hashCode() {
			 Integer i = new Integer(house.getId().toString() + aspectType.getId().toString());
			 return i.hashCode();
		 }
	}
}
