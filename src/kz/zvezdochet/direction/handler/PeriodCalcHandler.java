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
import java.util.TreeSet;

import org.eclipse.e4.core.contexts.Active;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.jfree.data.gantt.Task;
import org.jfree.data.gantt.TaskSeries;
import org.jfree.data.gantt.TaskSeriesCollection;
import org.jfree.data.time.Day;
import org.jfree.data.time.SimpleTimePeriod;
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
import kz.zvezdochet.bean.SkyPointAspect;
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
			boolean printable = periodPart.isPrintable();

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

			SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d MMMM yyyy");
			PdfWriter writer = null;
			Chapter chapter;
			Font font = null;
			AspectTypeService service = new AspectTypeService();
			Map<Long, List<TimeSeriesDataItem>> series = new HashMap<Long, List<TimeSeriesDataItem>>();
			boolean days = false;

			if (printable) {
				String filename = PlatformUtil.getPath(Activator.PLUGIN_ID, "/out/period.pdf").getPath();
				writer = PdfWriter.getInstance(doc, new FileOutputStream(filename));
		        writer.setPageEvent(new PageEventHandler(doc));
		        doc.open();

				font = PDFUtil.getRegularFont();

		        //metadata
		        PDFUtil.getMetaData(doc, "Прогноз событий");
	
		        //раздел
				chapter = new ChapterAutoNumber(PDFUtil.printHeader(new Paragraph(), "Общая информация"));
				chapter.setNumberDepth(0);
	
				String text = "Посуточный прогноз на период:\n";
				text += sdf.format(initDate);
				days = (DateUtil.getDateFromDate(initDate) != DateUtil.getDateFromDate(finalDate)
						|| DateUtil.getMonthFromDate(initDate) != DateUtil.getMonthFromDate(finalDate)
						|| DateUtil.getYearFromDate(initDate) != DateUtil.getYearFromDate(finalDate));
//				System.out.println(DateUtil.getDateFromDate(initDate) + "-" + DateUtil.getDateFromDate(finalDate) + "\t" + 
//						DateUtil.getMonthFromDate(initDate) + "-" + DateUtil.getMonthFromDate(finalDate) + "\t" +
//						DateUtil.getYearFromDate(initDate) + "-" + DateUtil.getYearFromDate(finalDate));
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
	
				chapter.add(new Paragraph("С помощью данного прогноза можно определить, какое время суток наиболее благоприятно для ваших планов.", font));
				chapter.add(Chunk.NEWLINE);
				chapter.add(new Paragraph("Прогноз классифицирует события по 3 признакам:", font));
	
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
				chapter.add(new Paragraph("Примечание", bfont));
				alist = new com.itextpdf.text.List(false, false, 10);
				alist.setNumbered(true);

				ListItem li = new ListItem();
		        li.add(new Chunk("Самые судьбоносные факторы, на которые стоит обратить внимание, перечислены в разделе «Важное». "
		        	+ "Если в числе важных указаны негативные события, необходимо откорректировать свои планы в указанных сферах жизни и не испытывать судьбу. "
			        + "Остальные события, даже из раздела «Напряжение», носят временный и не особо глубокий характер.", font));
		        alist.add(li);

		        li = new ListItem();
		        li.add(new Chunk("Важные, не повторяющиеся события говорят о главной повестке дня и задают тон всему дню. "
		        	+ "Они являются важным поводом, за которым уже следуют другие, второстепенные негативные и позитивные события.", font));
		        alist.add(li);

				li = new ListItem();
		        li.add(new Chunk("Если сфера жизни повторно упоминается в течение дня, значит она будет насыщена событиями, действиями и мыслями.", font));
		        alist.add(li);
	
				li = new ListItem();
		        li.add(new Chunk("Одна и та же сфера жизни в течение дня может проявиться и негативно и позитивно. "
		        	+ "Поэтому при составлении плана и ожидании событий учитывайте плюсы, минусы и указанные в тексте факторы, которые на них влияют.", font));
		        alist.add(li);
	
				li = new ListItem();
		        li.add(new Chunk("Ключевые события указывают на сферы жизни, которые могут вас волновать весь день. "
		        	+ "Если они повторяются изо дня в день, значит будут регулярно всплывать и потребуют не сиюминутного, а длительного разрешения.", font));
		        alist.add(li);
	
				li = new ListItem();
		        li.add(new Chunk("Максимальная погрешность прогноза события ±18 часов.", font));
		        alist.add(li);
	
		        chapter.add(alist);
				doc.add(chapter);
			}
			DirectionAspectService servicea = new DirectionAspectService();

			Map<String, Map<String, Map<Long, TreeSet<Long>>>> gitems = new HashMap<String, Map<String, Map<Long, TreeSet<Long>>>>();
			Map<String, Map<String, Map<String, TreeSet<Long>>>> hitems = new HashMap<String, Map<String, Map<String, TreeSet<Long>>>>();
			Map<String, Map<String, Map<String, TreeSet<Long>>>> pitems = new HashMap<String, Map<String, Map<String, TreeSet<Long>>>>();

			for (Date date = start.getTime(); start.before(end); start.add(Calendar.DATE, 1), date = start.getTime()) {
//				System.out.println(date);
				long time = date.getTime();

				Map<Long, Double> map = new HashMap<Long, Double>();
				Map<Integer, Map<Long, Set<PeriodItem>>> times = new HashMap<Integer, Map<Long, Set<PeriodItem>>>();
				Map<Integer, Set<PeriodItem>> ditems = new HashMap<Integer, Set<PeriodItem>>();
				Map<Long, Set<PeriodItem>> atems = new HashMap<Long, Set<PeriodItem>>();

				for (int i = 1; i < 5; i++) {
					int h = i * 6;
					String shour = (h < 10) ? "0" + h : String.valueOf(h);
					String sdate = DateUtil.formatCustomDateTime(date, "yyyy-MM-dd") + " " + shour + ":00:00";
//					System.out.println(shour);

					Event event = new Event();
					Date edate = DateUtil.getDatabaseDateTime(sdate);
					event.setBirth(edate);
					event.setPlace(place);
					event.setZone(zone);
					event.calc(false);

					Event prev = new Event();
					Calendar cal = Calendar.getInstance();
					cal.setTime(edate);
					cal.add(Calendar.DATE, -1);
					prev.setBirth(cal.getTime());
					prev.setPlace(place);
					prev.setZone(zone);
					prev.calc(false);

					List<Model> eplanets = event.getConfiguration().getPlanets();
					Map<Long, Set<PeriodItem>> items = new HashMap<Long, Set<PeriodItem>>();
					for (Model emodel : eplanets) {
						Planet eplanet = (Planet)emodel;
						//аспекты считаем только для утра
						if (1 == i) {
							for (Model model : planets) {
								Planet planet = (Planet)model;
								PeriodItem item = calc(eplanet, planet);
								if (null == item)
									continue;
								long id = item.aspect.getTypeid();
								Set<PeriodItem> list = atems.get(id);
								if (null == list)
									list = new HashSet<PeriodItem>();
								list.add(item);
								atems.put(id, list);
	
								double val = map.containsKey(id) ? map.get(id) : 0;
								map.put(id, val + 1);

								//собираем аспекты для диаграммы Гантта
								String pg = eplanet.getShortName();
								Map<String, Map<String, TreeSet<Long>>> pcats = pitems.get(pg);
								if (null == pcats)
									pcats = new HashMap<>();

								String p = planet.getShortName();
								Map<String, TreeSet<Long>> asps = pcats.get(p);
								if (null == asps)
									asps = new HashMap<>();

								String sign = " ";
								if (item.aspect.getType().getCode().equals("NEUTRAL"))
									sign = " × ";
								else if (item.aspect.getType().getCode().equals("NEGATIVE"))
									sign = " - ";
								else
									sign = " + ";
								String a = pg + sign + p;

								TreeSet<Long> dates = asps.get(a);
								if (null == dates)
									dates = new TreeSet<>();
								dates.add(time);
								asps.put(a, dates);
								pcats.put(p, asps);
								pitems.put(pg, pcats);
							}
						}
						for (Model model : houses) {
							House house = (House)model;
							PeriodItem item = calc(eplanet, house);
							if (null == item)
								continue;
							long id = item.aspect.getTypeid();

							//соблюдаем фильтр сфер жизни для аспектов кроме соединений
							if (!item.aspect.getCode().equals("CONJUNCTION"))
								if (!selhouses.contains(model.getId()))
									continue;

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

							//дома для диаграммы Гантта собираем только для утра
							if (1 == i) {
								String hg = house.getName();
								Map<String, Map<String, TreeSet<Long>>> pcats = hitems.get(hg);
								if (null == pcats)
									pcats = new HashMap<>();

								String p = item.planet.getName();
								Map<String, TreeSet<Long>> asps = pcats.get(p);
								if (null == asps)
									asps = new HashMap<>();

								String a = "";
								if (item.aspect.getType().getCode().equals("NEUTRAL"))
									a = item.planet.isGood() ? item.planet.getShortName() : item.planet.getNegative();
								else if (item.aspect.getType().getCode().equals("NEGATIVE"))
									a = item.planet.getNegative();
								else
									a = item.planet.getPositive();

								TreeSet<Long> dates = asps.get(a);
								if (null == dates)
									dates = new TreeSet<>();
								dates.add(time);
								asps.put(a, dates);
								pcats.put(p, asps);
								hitems.put(hg, pcats);
							}
						}
					}
					if (items != null && items.size() > 0)
						times.put(i, items);
				}

				if (times.size() > 0 || atems.size() > 0) {
					//определяем ключевые события (которые повторяются с утра до вечера)
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

						//собираем данные для диаграммы Гантта
						String p = item.aspect.getType().getCode().equals("NEGATIVE") ? item.planet.getNegative() : item.planet.getPositive();
						Map<String, Map<Long, TreeSet<Long>>> hcats = gitems.get(p);
						if (null == hcats)
							hcats = new HashMap<>();
						Map<Long, TreeSet<Long>> asps = hcats.get(item.house.getName());
						if (null == asps)
							asps = new HashMap<>();
						TreeSet<Long> dates = asps.get(id);
						if (null == dates)
							dates = new TreeSet<>();
						dates.add(time);
						asps.put(id, dates);
						hcats.put(item.house.getName(), asps);
						gitems.put(p, hcats);
					}
					times.put(0, items0);

					//формируем документ
					if (printable) {
						String sdfdate = sdf.format(date);
						chapter = new ChapterAutoNumber(PDFUtil.printHeader(new Paragraph(), sdfdate));
						chapter.setNumberDepth(0);
						Font fonth5 = PDFUtil.getHeaderFont();
	
						//аспекты
						Section section = PDFUtil.printSection(chapter, "Настроение");
						for (Map.Entry<Long, Set<PeriodItem>> entry : atems.entrySet()) {
							Set<PeriodItem> list = entry.getValue();
							if (null == list || 0 == list.size()) {
								section.add(new Paragraph("Нет данных", font));
								continue;
							}
							list = new LinkedHashSet<PeriodItem>(list);
							AspectType type = (AspectType)service.find(entry.getKey());
							section.add(new Paragraph(type.getDescription(), fonth5));
	
							String typeColor = type.getFontColor();
							BaseColor color = PDFUtil.htmlColor2Base(typeColor);
							Font afont = new Font(baseFont, 12, Font.NORMAL, color);
							Font abfont = new Font(baseFont, 12, Font.BOLD, color);
	
							com.itextpdf.text.List alist = new com.itextpdf.text.List(false, false, 10);
							for (PeriodItem item : list) {
								ListItem li = new ListItem();
								List<Model> texts = servicea.finds(new SkyPointAspect(item.planet, item.planet2, item.aspect));
								for (Model model : texts) {
									PlanetAspectText dirText = (PlanetAspectText)model;
									if (dirText != null) {
										li = new ListItem();
										Chunk chunk = new Chunk(item.planet.getShortName() + " " + type.getSymbol() + " " + item.planet2.getShortName() + ": ", abfont);
								        li.add(chunk);
										li.add(new Chunk(StringUtil.removeTags(dirText.getText()), afont));
										li.setSpacingAfter(10);
								        alist.add(li);
										
										List<TextGender> genders = dirText.getGenderTexts(female, child);
										for (TextGender gender : genders) {
											li = new ListItem();
									        li.add(new Chunk(PDFUtil.getGenderHeader(gender.getType()) + ": ", abfont));
											li.add(new Chunk(StringUtil.removeTags(gender.getText()), afont));
											li.setSpacingAfter(10);
									        alist.add(li);
										};
									}
								}
							}
							section.add(alist);
						}
	
						//дома
						SimpleDateFormat sdfshort = new SimpleDateFormat("d MMMM");
						String shortdate = sdfshort.format(date);
						for (Map.Entry<Integer, Map<Long, Set<PeriodItem>>> entry : times.entrySet()) {
							Map <Long, Set<PeriodItem>> items = entry.getValue();
							if (items != null && items.size() > 0) {
								int i = entry.getKey();
								String header = "";
								switch (i) {
									case 1: header = "Утро " + shortdate; break;
									case 2: header = "День " + shortdate; break;
									case 3: header = "Вечер " + shortdate; break;
									case 4: header = "Ночь " + shortdate; break;
									default: header = "Ключевые события дня " + shortdate;
								}
								section = PDFUtil.printSection(chapter, header);
								if (0 == i) {
									section.add(new Paragraph("Ключевые события могут произойти в любое время суток", font));
									section.add(Chunk.NEWLINE);
								}

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
									com.itextpdf.text.List alist = new com.itextpdf.text.List(false, false, 10);
	
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
										ListItem li = new ListItem();
										String text = "";
										String tcode = type.getCode();
										if (tcode.equals("NEGATIVE"))
											text = item.house.getNegative() + ". Причина – " + item.planet.getNegative();
										else if (tcode.equals("POSITIVE"))
											text = item.house.getPositive() + ". Фактор успеха – " + item.planet.getPositive();
										else {
											String s = "Контекст – " + (item.planet.isGood() ? item.planet.getPositive() : item.planet.getNegative());
											text = item.house.getDescription() + ". " + s;
										}
										Chunk chunk = new Chunk(text, new Font(baseFont, 12, Font.NORMAL, color));
								        li.add(new Chunk(item.house.getName() + ": ", new Font(baseFont, 12, Font.BOLD, color)));
								        li.add(chunk);
								        alist.add(li);
									}
									section.add(alist);
								}
							}
						}
						doc.add(chapter);
					}
				}
				for (Map.Entry<Long, Double> entry : map.entrySet()) {
					List<TimeSeriesDataItem> sitems = series.containsKey(entry.getKey()) ? series.get(entry.getKey()) : new ArrayList<TimeSeriesDataItem>();
					TimeSeriesDataItem tsdi = new TimeSeriesDataItem(new Day(date), entry.getValue());
					if (!sitems.contains(tsdi))
						sitems.add(tsdi);
					series.put(entry.getKey(), sitems);
				}
//				System.out.println();
			}

			if (printable) {
				if (days) {
					chapter = new ChapterAutoNumber(PDFUtil.printHeader(new Paragraph(), "Диаграммы"));
					chapter.setNumberDepth(0);
	
					//общая диаграмма
					Section section = PDFUtil.printSection(chapter, "Общие тенденции");
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
				    com.itextpdf.text.Image image = PDFUtil.printTimeChart(writer, "Общие тенденции", "Даты", "Баллы", dataset, 500, 0, true);
				    section.add(image);
				    chapter.add(Chunk.NEXTPAGE);

					//общая диаграмма Гантта
					section = PDFUtil.printSection(chapter, "Ключевые периоды");
			        final TaskSeriesCollection collection = new TaskSeriesCollection();
					for (Map.Entry<String, Map<String, Map<Long, TreeSet<Long>>>> pentry : gitems.entrySet()) {
						Map<String, Map<Long, TreeSet<Long>>> hcats = pentry.getValue();
						if (null == hcats || 0 == hcats.size())
							continue;
						final TaskSeries s = new TaskSeries(pentry.getKey());

						for (Map.Entry<String, Map<Long, TreeSet<Long>>> hentry : hcats.entrySet()) {
							Map<Long, TreeSet<Long>> asps = hentry.getValue();
							if (null == asps || 0 == asps.size())
								continue;

							//с учётом того, что в диаграмме Гантта может иметь место только один отрезок времени
							//данной категории в серии, то рассматриваем только первое вхождение планеты-дома в общий период
							for (Map.Entry<Long, TreeSet<Long>> aentry : asps.entrySet()) {
								TreeSet<Long> dates = aentry.getValue();
								if (null == dates || dates.size() < 2)
									continue;
	
								long initdate = dates.first();
								long finaldate = dates.last();
								if (finaldate > initdate)
									s.add(new Task(hentry.getKey(), new SimpleTimePeriod(new Date(initdate), new Date(finaldate))));
							}
						}
						if (!s.isEmpty())
							collection.add(s);
					}
				    image = PDFUtil.printGanttChart(writer, "Ключевые периоды", "", "", collection, 0, 700, true);
				    section.add(image);
				    chapter.add(Chunk.NEXTPAGE);

					//диаграммы Гантта по аспектам
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
							section = PDFUtil.printSection(chapter, title);
						    image = PDFUtil.printGanttChart(writer, title, "", "", collectiona, 0, 0, false);
						    section.add(image);
						}
					}
				    chapter.add(Chunk.NEXTPAGE);

					//диаграммы Гантта по домам
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
							section = PDFUtil.printSection(chapter, hgentry.getKey());
						    image = PDFUtil.printGanttChart(writer, hgentry.getKey(), "", "", collectionh, 0, 0, true);
						    section.add(image);
						}
					}
					doc.add(chapter);
				}			
				doc.add(Chunk.NEWLINE);
		        doc.add(PDFUtil.printCopyright());
			}
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
			double res = CalcUtil.getDifference(point1.getCoord(), point2.getCoord());

			//определяем, является ли аспект стандартным
			List<Model> aspects = new AspectService().getMajorList();
			for (Model realasp : aspects) {
				Aspect a = (Aspect)realasp;
				if (a.isMain() && a.isExact(res)) {
					if (a.getPlanetid() > 0)
						continue;

//					AspectType type = a.getType();
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
		} catch (DataAccessException e) {
			e.printStackTrace();
		}
		return null;
	}
}
