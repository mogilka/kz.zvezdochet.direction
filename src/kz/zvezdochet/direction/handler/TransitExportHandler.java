package kz.zvezdochet.direction.handler;

import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.e4.core.contexts.Active;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.jfree.data.gantt.Task;
import org.jfree.data.gantt.TaskSeries;
import org.jfree.data.gantt.TaskSeriesCollection;
import org.jfree.data.time.SimpleTimePeriod;

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
import kz.zvezdochet.core.service.DataAccessException;
import kz.zvezdochet.core.util.CalcUtil;
import kz.zvezdochet.core.util.DateUtil;
import kz.zvezdochet.core.util.PlatformUtil;
import kz.zvezdochet.direction.Activator;
import kz.zvezdochet.direction.bean.PeriodItem;
import kz.zvezdochet.direction.part.TransitPart;
import kz.zvezdochet.export.handler.PageEventHandler;
import kz.zvezdochet.export.util.PDFUtil;
import kz.zvezdochet.service.AspectService;
/**
 * Обработчик экспорта транзитов на указанный период
 * @author Natalie Didenko
 */
public class TransitExportHandler extends Handler {
	private BaseFont baseFont;

	public TransitExportHandler() {
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
			TransitPart periodPart = (TransitPart)activePart.getObject();
			if (!periodPart.check(0)) return;
			Event person = periodPart.getPerson();
			Place place = periodPart.getPlace();
			double zone = periodPart.getZone();

			Object[] spheres = periodPart.getSpheres();
			List<Long> selhouses = new ArrayList<>();
			List<Long> selplanets = new ArrayList<>();
			SphereService sphereService = new SphereService();
			for (Object item : spheres) {
				Sphere sphere = (Sphere)item;
				List<Model> houses = sphereService.getHouses(sphere.getId());
				for (Model model : houses)
					if (!selhouses.contains(model.getId()))
						selhouses.add(model.getId());

				List<Model> planets = sphereService.getPlanets(sphere.getId());
				for (Model model : planets)
					if (!selplanets.contains(model.getId()))
						selplanets.add(model.getId());
			}

			Collection<Planet> planets = person.getPlanets().values();
			Collection<House> houses = person.getHouses().values();
	
			updateStatus("Расчёт транзитов на период", false);
	
			Date initDate = periodPart.getInitialDate();
			Date finalDate = periodPart.getFinalDate();
			Calendar start = Calendar.getInstance();
			start.setTime(initDate);
			Calendar end = Calendar.getInstance();
			end.setTime(finalDate);

			SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d MMMM yyyy");
			PdfWriter writer = null;
			Chapter chapter;
			Font font = null;
			boolean days = false;

			String filename = PlatformUtil.getPath(Activator.PLUGIN_ID, "/out/period.pdf").getPath();
			writer = PdfWriter.getInstance(doc, new FileOutputStream(filename));
	        writer.setPageEvent(new PageEventHandler());
	        doc.open();

			font = PDFUtil.getRegularFont();

	        //metadata
	        PDFUtil.getMetaData(doc, "Прогноз событий");
	
	        //раздел
			chapter = new ChapterAutoNumber(PDFUtil.printHeader(new Paragraph(), "Общая информация", null));
			chapter.setNumberDepth(0);
	
			String text = person.getCallname() + " – Прогноз тенденций на период:\n";
			text += sdf.format(initDate);
			days = (DateUtil.getDateFromDate(initDate) != DateUtil.getDateFromDate(finalDate)
				|| DateUtil.getMonthFromDate(initDate) != DateUtil.getMonthFromDate(finalDate)
				|| DateUtil.getYearFromDate(initDate) != DateUtil.getYearFromDate(finalDate));
			if (days)
				text += " — " + sdf.format(finalDate);
			Paragraph p = new Paragraph(text, font);
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
	
			chapter.add(new Paragraph("Данный прогноз показывает благоприятные и неблагоприятные периоды для тех или иных сфер жизни", font));
			doc.add(chapter);

			Map<String, Map<String, Map<String, TreeSet<Long>>>> hitems = new HashMap<String, Map<String, Map<String, TreeSet<Long>>>>();
			Map<String, Map<String, Map<String, TreeSet<Long>>>> pitems = new HashMap<String, Map<String, Map<String, TreeSet<Long>>>>();

			for (Date date = start.getTime(); start.before(end); start.add(Calendar.DATE, 1), date = start.getTime()) {
				long time = date.getTime();

				Event event = new Event();
				String sdate = DateUtil.formatCustomDateTime(date, "yyyy-MM-dd") + " 00:00:00";
				Date edate = DateUtil.getDatabaseDateTime(sdate);
				event.setBirth(edate);
				event.setPlace(place);
				event.setZone(zone);
				event.calc(false);

				Map<Long, Set<PeriodItem>> htems = new HashMap<Long, Set<PeriodItem>>();
				Map<Long, Set<PeriodItem>> atems = new HashMap<Long, Set<PeriodItem>>();

				Collection<Planet> eplanets = event.getPlanets().values();
				for (Planet eplanet : eplanets) {
					for (Planet planet : planets) {
						if (!selplanets.isEmpty()) {
							if (!selplanets.contains(planet.getId()) && !selplanets.contains(eplanet.getId()))
								continue;
						}
						PeriodItem item = calc(eplanet, planet);
						if (item != null) {
							long id = item.aspect.getTypeid();
							Set<PeriodItem> list = atems.get(id);
							if (null == list)
								list = new HashSet<PeriodItem>();
							list.add(item);
							atems.put(id, list);

							//собираем аспекты для диаграммы Гантта
							String pg = eplanet.getMark(null) + " " + eplanet.getShortName();
							Map<String, Map<String, TreeSet<Long>>> pcats = pitems.get(pg);
							if (null == pcats)
								pcats = new HashMap<>();

							String pg2 = planet.getShortName();
							Map<String, TreeSet<Long>> asps = pcats.get(pg2);
							if (null == asps)
								asps = new HashMap<>();

							String sign = Math.round(item.aspect.getValue()) + "°";
							String a = pg + " " + sign + " " + pg2;

							TreeSet<Long> dates = asps.get(a);
							if (null == dates)
								dates = new TreeSet<>();
							dates.add(time);
							asps.put(a, dates);
							pcats.put(pg2, asps);
							pitems.put(pg, pcats);
						}
					}
					for (Model model : houses) {
						House house = (House)model;
						PeriodItem item = calc(eplanet, house);
						if (null == item)
							continue;

						if (!selhouses.isEmpty() && !selhouses.contains(model.getId()))
							continue;

						long id = item.aspect.getTypeid();
						Set<PeriodItem> list = htems.get(id);
						if (null == list)
							list = new HashSet<PeriodItem>();
						list.add(item);
						htems.put(id, list);

						//собираем дома для диаграммы Гантта
						String hg = house.getName();
						Map<String, Map<String, TreeSet<Long>>> pcats = hitems.get(hg);
						if (null == pcats)
							pcats = new HashMap<>();

						String pg = eplanet.getName();
						Map<String, TreeSet<Long>> asps = pcats.get(pg);
						if (null == asps)
							asps = new HashMap<>();

						String a = eplanet.getMark(null) + " " + Math.round(item.aspect.getValue()) + "° ";
						if (item.aspect.getType().getCode().equals("NEUTRAL"))
							a += eplanet.getShortName();
						else if (item.aspect.getType().getCode().equals("NEGATIVE"))
							a += eplanet.getNegative();
						else
							a += eplanet.getPositive();

						TreeSet<Long> dates = asps.get(a);
						if (null == dates)
							dates = new TreeSet<>();
						dates.add(time);
						asps.put(a, dates);
						pcats.put(pg, asps);
						hitems.put(hg, pcats);
					}
				}
			}

			//диаграммы Гантта по аспектам
			chapter = new ChapterAutoNumber(PDFUtil.printHeader(new Paragraph(), "Тенденции личности", null));
			chapter.setNumberDepth(0);

			for (Map.Entry<String, Map<String, Map<String, TreeSet<Long>>>> pgentry : pitems.entrySet()) {
		        final TaskSeriesCollection collectiona = new TaskSeriesCollection();
		        Map<String, Map<String, TreeSet<Long>>> pcats = pgentry.getValue();
			    if (null == pcats || 0 == pcats.size())
			    	continue;
			    for (Map.Entry<String, Map<String, TreeSet<Long>>> pentry : pcats.entrySet()) {
			    	Map<String, TreeSet<Long>> asps = pentry.getValue();
			    	if (null == asps || 0 == asps.size())
			    		continue;

					//с учётом того, что в диаграмме Гантта может иметь место только один отрезок времени
					//данной категории в серии, то рассматриваем только первое вхождение планеты-дома в общий период
					TaskSeries s = null;
					for (Map.Entry<String, TreeSet<Long>> aentry : asps.entrySet()) {
						s = new TaskSeries(aentry.getKey());
						TreeSet<Long> dates = aentry.getValue();
						if (null == dates)
							continue;

						long initdate = dates.first();
						long finaldate = (1 == dates.size()) ? initdate + 86400000 : dates.last();
						s.add(new Task(aentry.getKey(), new SimpleTimePeriod(new Date(initdate), new Date(finaldate))));
					}
					if (s != null && !s.isEmpty())
						collectiona.add(s);
				}
				int cnt = collectiona.getSeriesCount();
				if (cnt > 0) {
					String title = pgentry.getKey();
					Section section = PDFUtil.printSection(chapter, title, null);
				    Image image = PDFUtil.printGanttChart(writer, title, "", "", collectiona, 0, 0, false);
				    section.add(image);
				}
			}
		    chapter.add(Chunk.NEXTPAGE);
			doc.add(chapter);

			//диаграммы Гантта по домам
			chapter = new ChapterAutoNumber(PDFUtil.printHeader(new Paragraph(), "Тенденции событий", null));
			chapter.setNumberDepth(0);

			for (Map.Entry<String, Map<String, Map<String, TreeSet<Long>>>> hgentry : hitems.entrySet()) {
		        final TaskSeriesCollection collectionh = new TaskSeriesCollection();
		        Map<String, Map<String, TreeSet<Long>>> pcats = hgentry.getValue();
		        if (null == pcats || 0 == pcats.size())
		        	continue;
				for (Map.Entry<String, Map<String, TreeSet<Long>>> pentry : pcats.entrySet()) {
					Map<String, TreeSet<Long>> asps = pentry.getValue();
					if (null == asps || 0 == asps.size())
						continue;

					//с учётом того, что в диаграмме Гантта может иметь место только один отрезок времени
					//данной категории в серии, то рассматриваем только первое вхождение планеты-дома в общий период
					TaskSeries s = null;
					for (Map.Entry<String, TreeSet<Long>> aentry : asps.entrySet()) {
						s = new TaskSeries(aentry.getKey());
						TreeSet<Long> dates = aentry.getValue();
						if (null == dates)
							continue;

						long initdate = dates.first();
						long finaldate = (1 == dates.size()) ? initdate + 86400000 : dates.last();
						s.add(new Task(aentry.getKey(), new SimpleTimePeriod(new Date(initdate), new Date(finaldate))));
					}
					if (s != null && !s.isEmpty())
						collectionh.add(s);
				}
				if (collectionh.getSeriesCount() > 0) {
					Section section = PDFUtil.printSection(chapter, hgentry.getKey(), null);
					Image image = PDFUtil.printGanttChart(writer, hgentry.getKey(), "", "", collectionh, 0, 0, true);
				    section.add(image);
				}
			}
			doc.add(chapter);
			doc.add(Chunk.NEWLINE);
	        doc.add(PDFUtil.printCopyright());
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			if (doc != null)
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
			double res = CalcUtil.getDifference(point1.getLongitude(), point2.getLongitude());
			AspectService service = new AspectService();

			//определяем, является ли аспект стандартным
			List<Model> aspects = service.getMajorList();
			for (Model realasp : aspects) {
				Aspect a = (Aspect)realasp;
				if (a.isExact(res)) {
					if (a.getPlanetid() > 0)
						continue;

					if (point2 instanceof Planet)
						if (a.getCode().equals("OPPOSITION") &&
								(point2.getCode().equals("Kethu") || point2.getCode().equals("Rakhu")))
							continue;

					PeriodItem item = new PeriodItem();
					item.aspect = a;
					item.planet = (Planet)point1;
					if (point2 instanceof Planet) {
						Planet planet2 = (Planet)point2;
						item.planet2 = planet2;
						item.house = planet2.getHouse();
					} else if (point2 instanceof House)
						item.house = (House)point2;
//					System.out.println(point1.getName() + " " + type.getSymbol() + " " + point2.getName());
					return item;
				}
			}
		} catch (DataAccessException e) {
			e.printStackTrace();
		}
		return null;
	}
}
