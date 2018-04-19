package kz.zvezdochet.direction.handler;

import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.e4.core.contexts.Active;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.TimeSeriesDataItem;

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

import kz.zvezdochet.analytics.bean.PlanetAspectText;
import kz.zvezdochet.analytics.bean.Sphere;
import kz.zvezdochet.analytics.service.SphereService;
import kz.zvezdochet.bean.Aspect;
import kz.zvezdochet.bean.AspectType;
import kz.zvezdochet.bean.Event;
import kz.zvezdochet.bean.House;
import kz.zvezdochet.bean.Place;
import kz.zvezdochet.bean.Planet;
import kz.zvezdochet.bean.SkyPoint;
import kz.zvezdochet.core.bean.Model;
import kz.zvezdochet.core.bean.TextGender;
import kz.zvezdochet.core.handler.Handler;
import kz.zvezdochet.core.service.DataAccessException;
import kz.zvezdochet.core.util.CalcUtil;
import kz.zvezdochet.core.util.DateUtil;
import kz.zvezdochet.core.util.PlatformUtil;
import kz.zvezdochet.core.util.StringUtil;
import kz.zvezdochet.direction.Activator;
import kz.zvezdochet.direction.bean.PeriodItem;
import kz.zvezdochet.direction.part.PeriodPart;
import kz.zvezdochet.direction.service.DirectionAspectService;
import kz.zvezdochet.export.handler.PageEventHandler;
import kz.zvezdochet.export.util.PDFUtil;
import kz.zvezdochet.service.AspectService;
import kz.zvezdochet.service.AspectTypeService;
import kz.zvezdochet.util.Configuration;
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
			boolean female = person.isFemale();
			boolean child = person.isChild();

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
			Chapter chapter = new ChapterAutoNumber(PDFUtil.printHeader(new Paragraph(), "Общая информация"));
			chapter.setNumberDepth(0);

			String text = "Посуточный прогноз на период:\n";
			SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d MMMM yyyy");
			text += sdf.format(initDate);
			boolean days = (DateUtil.getDateFromDate(initDate) != DateUtil.getDateFromDate(finalDate)
					|| DateUtil.getMonthFromDate(initDate) != DateUtil.getMonthFromDate(finalDate)
					|| DateUtil.getYearFromDate(initDate) != DateUtil.getYearFromDate(finalDate));
			System.out.println(DateUtil.getDateFromDate(initDate) + "-" + DateUtil.getDateFromDate(finalDate) + "\t" + 
					DateUtil.getMonthFromDate(initDate) + "-" + DateUtil.getMonthFromDate(finalDate) + "\t" +
					DateUtil.getYearFromDate(initDate) + "-" + DateUtil.getYearFromDate(finalDate));
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

			chapter.add(new Paragraph("С помощью данного прогноза можно определить, какое время суток наиболее благоприятно для ваших планов.", font));
			chapter.add(Chunk.NEWLINE);
			chapter.add(new Paragraph("Прогноз классифицирует события по 3 признакам:", font));

			AspectTypeService service = new AspectTypeService();
			List<AspectType> types = service.getMainList();
			Font bfont = new Font(baseFont, 12, Font.BOLD, PDFUtil.FONTCOLOR);
			com.itextpdf.text.List alist = new com.itextpdf.text.List(false, false, 10);
			for (AspectType aspectType : types) {
				if (aspectType.getId() > 3)
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

			chapter.add(Chunk.NEWLINE);
			chapter.add(new Paragraph("Примечание", font));
			alist = new com.itextpdf.text.List(false, false, 10);
			ListItem li = new ListItem();
	        li.add(new Chunk("Если сфера жизни повторно упоминается в течение дня, значит она будет насыщена событиями, действиями и мыслями.", font));
	        alist.add(li);

			li = new ListItem();
	        li.add(new Chunk("Одна и та же сфера жизни в течение дня может проявиться и негативно и позитивно. "
	        	+ "Поэтому при составлении плана и ожидании событий учитывайте плюсы, минусы и указанные в тексте факторы, которые на них влияют.", font));
	        alist.add(li);
	        chapter.add(alist);
			doc.add(chapter);

			DirectionAspectService servicea = new DirectionAspectService();

			for (Date date = start.getTime(); start.before(end); start.add(Calendar.DATE, 1), date = start.getTime()) {
				System.out.println(date);

				Map<Long, Double> map = new HashMap<Long, Double>();
				Map<Integer, Map<Long, Set<PeriodItem>>> times = new HashMap<Integer, Map<Long, Set<PeriodItem>>>();
				Map<Integer, Set<PeriodItem>> ditems = new HashMap<Integer, Set<PeriodItem>>();

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
					prev.calc(true);

					List<Planet> iplanets = new ArrayList<Planet>();
					List<Model> eplanets = event.getConfiguration().getPlanets();
					for (Model model : eplanets) {
						Planet planet = (Planet)model;
						List<Object> ingresses = planet.isIngressed(prev, event);
						if (ingresses != null && ingresses.size() > 0)
							iplanets.add(planet);
					}

					Map<Long, Set<PeriodItem>> items = new HashMap<Long, Set<PeriodItem>>();
					for (Planet eplanet : iplanets) {
						for (Model model : planets) {
							Planet planet = (Planet)model;
							PeriodItem item = calc(eplanet, planet);
							if (null == item)
								continue;
							long id = item.aspect.getTypeid();
							Set<PeriodItem> list = items.get(id);
							if (null == list)
								list = new HashSet<PeriodItem>();
							list.add(item);
							items.put(id, list);

							double val = map.containsKey(id) ? map.get(id) : 0;
							map.put(id, val + 1);
						}
						for (Model model : houses) {
							if (!selhouses.contains(model.getId()))
								continue;
							House house = (House)model;
							PeriodItem item = calc(eplanet, house);
							if (null == item)
								continue;
							long id = item.aspect.getTypeid();
							Set<PeriodItem> list = items.get(id);
							if (null == list)
								list = new HashSet<PeriodItem>();
							list.add(item);
							items.put(id, list);

							double val = map.containsKey(id) ? map.get(id) : 0;
							map.put(id, val + 1);

							//собираем отдельно список для последующего вычисления ключевых событий дня
							list = ditems.get(i);
							if (null == list)
								list = new HashSet<PeriodItem>();
							list.add(item);
							ditems.put(i, list);
						}
					}
					if (items != null && items.size() > 0)
						times.put(i, items);
				}

				if (times.size() > 0) {
					//определяем ключевые события
					Set<PeriodItem> set0 = new HashSet<>();
					Set<PeriodItem> allitems = new HashSet<>();
					Collection<Set<PeriodItem>> allsets = ditems.values();
					for (Set<PeriodItem> set : allsets)
						for (PeriodItem item : set)
							allitems.add(item);
					allsets = null;

					Set<PeriodItem> dItems1 = ditems.get(1);
					Set<PeriodItem> dItems2 = ditems.get(2);
					Set<PeriodItem> dItems3 = ditems.get(3);
					Set<PeriodItem> dItems4 = ditems.get(4);
					for (PeriodItem item : allitems) {
						if (dItems1 != null && dItems1.contains(item)
								&& dItems2 != null && dItems2.contains(item)
								&& dItems3 != null && dItems3.contains(item)
								&& dItems4 != null && dItems4.contains(item))
							set0.add(item);
					}
					allitems = null; dItems1 = null; dItems2 = null; dItems3 = null; dItems4 = null;

					Map<Long, Set<PeriodItem>> items0 = new HashMap<Long, Set<PeriodItem>>();
					for (PeriodItem item : set0) {
						long id = item.aspect.getTypeid();
						Set<PeriodItem> list = items0.get(id);
						if (null == list)
							list = new HashSet<PeriodItem>();
						list.add(item);
						items0.put(id, list);
					}
					times.put(0, items0);

					//формируем документ
					String sdfdate = sdf.format(date);
					chapter = new ChapterAutoNumber(PDFUtil.printHeader(new Paragraph(), sdfdate));
					chapter.setNumberDepth(0);
	
					for (Map.Entry<Integer, Map<Long, Set<PeriodItem>>> entry : times.entrySet()) {
						Map <Long, Set<PeriodItem>> items = entry.getValue();
						if (items != null && items.size() > 0) {
							int i = entry.getKey();
							String header = "";
							switch (i) {
								case 1: header = "Утро"; break;
								case 2: header = "День"; break;
								case 3: header = "Вечер"; break;
								case 4: header = "Ночь"; break;
								default: header = "Ключевые события дня";
							}
							Section section = PDFUtil.printSection(chapter, header);
							if (0 == i) {
								section.add(new Paragraph("Ключевые события могут произойти в любое время суток", font));
								section.add(Chunk.NEWLINE);
							}

							Font fonth5 = PDFUtil.getHeaderFont();
							for (Map.Entry<Long, Set<PeriodItem>> entry2 : items.entrySet()) {
								Set<PeriodItem> list = entry2.getValue();
								if (null == list || 0 == list.size()) {
									section.add(new Paragraph("Нет данных", font));
									continue;
								}
								list = new LinkedHashSet<PeriodItem>(list);
								AspectType type = (AspectType)service.find(entry2.getKey());
								section.add(new Paragraph(type.getDescription(), fonth5));
		
								String typeColor = type.getFontColor();
								BaseColor color = PDFUtil.htmlColor2Base(typeColor);
								alist = new com.itextpdf.text.List(false, false, 10);

								if (i > 0) {
									Set<PeriodItem> list2 = new HashSet<>();
									for (PeriodItem item : list)
										if (set0.contains(item))
											continue;
										else
											list2.add(item);
									list = new LinkedHashSet<PeriodItem>(list2);
								}
								if (0 == list.size())
									section.add(new Paragraph("См. ключевые события", font));
								else for (PeriodItem item : list) {
									li = new ListItem();
									text = "";
									String tcode = type.getCode();
									if (tcode.equals("NEGATIVE"))
										text = item.house.getNegative() + ". Причина – " + item.planet.getNegative();
									else if (tcode.equals("POSITIVE"))
										text = item.house.getPositive() + ". Фактор успеха – " + item.planet.getPositive();
									else {
										String s = "Контекст – " + (item.planet.isGood() ? item.planet.getPositive() : item.planet.getNegative());
										text = item.house.getDescription() + ". " + s;
									}
							        chunk = new Chunk(text, new Font(baseFont, 12, Font.NORMAL, color));
							        li.add(new Chunk(item.house.getName() + ": ", new Font(baseFont, 12, Font.BOLD, color)));
							        li.add(chunk);
							        alist.add(li);

									if (item.planet2 != null) {
										PlanetAspectText dirText = (PlanetAspectText)servicea.find(item.planet, item.planet2, item.aspect);
										if (dirText != null) {
											li = new ListItem();
									        chunk = new Chunk(item.planet.getShortName() + " " + type.getSymbol() + " " + item.planet2.getShortName() + ": ", bfont);
									        li.add(chunk);
											li.add(new Chunk(StringUtil.removeTags(dirText.getText()), font));
									        alist.add(li);
											
											List<TextGender> genders = dirText.getGenderTexts(female, child);
											for (TextGender gender : genders) {
												li = new ListItem();
										        li.add(new Chunk(PDFUtil.getGenderHeader(gender.getType()) + ": ", bfont));
												li.add(new Chunk(StringUtil.removeTags(gender.getText()), font));
										        alist.add(li);
											};
										}
									}
								}
								section.add(alist);
							}
						}
					}
					doc.add(chapter);
				}
				for (Map.Entry<Long, Double> entry : map.entrySet()) {
					if (1 == entry.getKey())
						continue;
					List<TimeSeriesDataItem> sitems = series.containsKey(entry.getKey()) ? series.get(entry.getKey()) : new ArrayList<TimeSeriesDataItem>();
					TimeSeriesDataItem tsdi = new TimeSeriesDataItem(new Day(date), entry.getValue());
					if (!sitems.contains(tsdi))
						sitems.add(tsdi);
					series.put(entry.getKey(), sitems);
				}
				System.out.println();
			}

			if (days) {
				chapter = new ChapterAutoNumber(PDFUtil.printHeader(new Paragraph(), "Диаграммы"));
				chapter.setNumberDepth(0);

				//общая диаграмма
				TimeSeriesCollection dataset = new TimeSeriesCollection();
				for (Map.Entry<Long, List<TimeSeriesDataItem>> entry : series.entrySet()) {
					List<TimeSeriesDataItem> sitems = entry.getValue();
					if (null == sitems || 0 == sitems.size())
						continue;
					AspectType asptype = (AspectType)service.find(entry.getKey());
					if (null == asptype.getDescription())
						continue;
					TimeSeries timeSeries = new TimeSeries(asptype.getDescription());
					for (TimeSeriesDataItem tsdi : sitems)
						timeSeries.add(tsdi);
					dataset.addSeries(timeSeries);
				}
			    com.itextpdf.text.Image image = PDFUtil.printTimeChart(writer, "Прогноз периода", "Даты", "Баллы", dataset, 500, 0, true);
				chapter.add(image);
				doc.add(chapter);
			}			
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

			//определяем, является ли аспект стандартным
			List<Model> aspects = new AspectService().getMajorList();
			for (Model realasp : aspects) {
				Aspect a = (Aspect)realasp;
				if (a.isMain() && a.isExact(res)) {
					if (a.getPlanetid() > 0)
						continue;

					AspectType type = a.getType();
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
					System.out.println(point1.getName() + " " + type.getSymbol() + " " + point2.getName());
					return item;
				}
			}
		} catch (DataAccessException e) {
			e.printStackTrace();
		}
		return null;
	}
}
