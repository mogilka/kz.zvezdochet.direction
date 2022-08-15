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

import com.itextpdf.text.BaseColor;
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

import kz.zvezdochet.analytics.bean.PlanetAspectText;
import kz.zvezdochet.bean.AspectType;
import kz.zvezdochet.bean.Event;
import kz.zvezdochet.bean.House;
import kz.zvezdochet.bean.Ingress;
import kz.zvezdochet.bean.Place;
import kz.zvezdochet.bean.Planet;
import kz.zvezdochet.bean.SkyPoint;
import kz.zvezdochet.bean.SkyPointAspect;
import kz.zvezdochet.core.handler.Handler;
import kz.zvezdochet.core.ui.util.DialogUtil;
import kz.zvezdochet.core.util.DateUtil;
import kz.zvezdochet.core.util.OsUtil;
import kz.zvezdochet.core.util.PlatformUtil;
import kz.zvezdochet.direction.Activator;
import kz.zvezdochet.direction.bean.DirectionText;
import kz.zvezdochet.direction.part.TransitPart;
import kz.zvezdochet.direction.service.DirectionAspectService;
import kz.zvezdochet.direction.service.DirectionService;
import kz.zvezdochet.export.bean.Bar;
import kz.zvezdochet.export.handler.PageEventHandler;
import kz.zvezdochet.export.util.PDFUtil;
import kz.zvezdochet.service.AspectTypeService;

/**
 * Генерация планетарных циклов за указанный период
 * @author Natalie Didenko
 */
public class TransitCycleHandler extends Handler {
	private BaseFont baseFont;

	public TransitCycleHandler() {
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
			Map<Long, House> houses = person.getHouses();
			int choice = DialogUtil.alertQuestion("Вопрос", "Выберите тип прогноза:", new String[] {"Реалистичный", "Оптимистичный"});
			boolean optimistic = choice > 0;
			int category = DialogUtil.alertQuestion("Вопрос", "Выберите тип прогноза:", new String[] {"Полный", "Работа"});

			//Признак использования астрологических терминов
			boolean term = periodPart.isTerm();
			updateStatus("Расчёт циклов на период", false);

			Date initDate = periodPart.getInitialDate();
			Date finalDate = periodPart.getFinalDate();
			Calendar start = Calendar.getInstance();
			start.setTime(initDate);
			Calendar end = Calendar.getInstance();
			end.setTime(finalDate);
			end.add(Calendar.DATE, 1);
			end.set(Calendar.HOUR_OF_DAY, 0);
			end.set(Calendar.MINUTE, 0);
			end.set(Calendar.SECOND, 0);
			long startime = initDate.getTime();
			long endtime = finalDate.getTime();

			String filename = PlatformUtil.getPath(Activator.PLUGIN_ID, "/out/cycle.pdf").getPath();
			PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(filename));
	        writer.setPageEvent(new PageEventHandler());
	        doc.open();

	    	Font font = PDFUtil.getRegularFont();

	        //metadata
	        PDFUtil.getMetaData(doc, "Важные периоды");

	        //раздел
			Chapter chapter = new ChapterAutoNumber("Важные периоды");
			chapter.setNumberDepth(0);

			//шапка
			Paragraph p = new Paragraph();
			PDFUtil.printHeader(p, "Важные периоды", null);
			chapter.add(p);

			String text = person.getCallname() + ", прогноз на период: ";
			SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d MMMM yyyy");
			SimpleDateFormat spf = new SimpleDateFormat("d MMMM");
			text += sdf.format(initDate);
			boolean days = (DateUtil.getDateFromDate(initDate) != DateUtil.getDateFromDate(finalDate)
				|| DateUtil.getMonthFromDate(initDate) != DateUtil.getMonthFromDate(finalDate)
				|| DateUtil.getYearFromDate(initDate) != DateUtil.getYearFromDate(finalDate));
			if (days)
				text += " — " + sdf.format(finalDate);
			p = new Paragraph(text, font);
	        p.setAlignment(Element.ALIGN_CENTER);
			chapter.add(p);

			text = "Тип прогноза: " + (optimistic ? "оптимистичный" : "реалистичный");
			if (category > 0) {
				if (1 == category)
					text += ", категория «Работа»"; 
			}
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

			p = new Paragraph();
			p.add(new Chunk("Файл содержит большой объём информации, и нет смысла пытаться его весь прочитать. "
				+ "Используйте прогноз в начале каждого месяца как путеводитель, помогающий понять тенденции и учесть риски. ", font));
	        chunk = new Chunk("Пояснения к прогнозу", new Font(baseFont, 12, Font.UNDERLINE, PDFUtil.FONTCOLOR));
	        chunk.setAnchor("https://zvezdochet.guru/post/223/poyasnenie-k-ezhednevnym-prognozam");
	        p.add(chunk);
			chapter.add(p);
			chapter.add(Chunk.NEWLINE);

			Font red = PDFUtil.getDangerFont();
			p = new Paragraph();
			String divergence = person.isRectified() ? "1 день" : "2 дня";
			p.add(new Chunk("Общая погрешность прогноза составляет ±" + divergence + ". ", red));
			chapter.add(p);
			chapter.add(Chunk.NEWLINE);

			chapter.add(new Paragraph("Если длительность прогноза исчисляется днями, неделями и месяцами, то это не значит, что каждый день будет что-то происходить. "
	        	+ "Просто вероятность описанных событий будет сохраняться в течение всего периода. "
	        	+ "Чаще всего прогноз ярко проявляет себя в первый же день периода, но может сбыться и позже.", font));

			p = new Paragraph("Если в прогнозе упомянуты люди, которых уже нет в живых (родители, супруги, родственники), "
				+ "значит речь идёт о людях, их заменяющих (опекуны, крёстные) или похожих на них по характеру.", font);
			p.setSpacingBefore(10);
			chapter.add(p);
			chapter.add(Chunk.NEWLINE);

			Font bold = new Font(baseFont, 12, Font.BOLD);
			chapter.add(new Paragraph("Примечание:", bold));
			com.itextpdf.text.List list = new com.itextpdf.text.List(false, false, 10);
	        ListItem li = new ListItem();
	        li.add(new Chunk("Чёрным цветом выделено самое важное, что с вами произойдёт.", font));
	        list.add(li);

			Font green = PDFUtil.getSuccessFont();
			li = new ListItem();
	        li.add(new Chunk("Зелёным цветом выделены позитивные тенденции. "
	        	+ "К ним относятся события, которые сами по себе удачно складываются " 
	        	+ "и представляют собой благоприятные возможности, наполняющие вас энергией. Их надо использовать по максимуму.", green));
	        list.add(li);

	        if (!optimistic) {
				li = new ListItem();
		        li.add(new Chunk("Красным цветом выделены негативные тенденции, которые потребуют расхода энергии. "
		        	+ "Они указывают на сферы, от которых не нужно ждать многого. "
		        	+ "Это признак того, что вам необходим отдых, переосмысление и мобилизация ресурсов для решения проблемы.", red));
		        list.add(li);
	        }
	        chapter.add(list);

	        chapter.add(Chunk.NEWLINE);
	        chapter.add(new Paragraph("Заголовки абзацев (например, «Добро + Прибыль») используются для структурирования текста и "
	        	+ "указывают на сферу жизни, к которой относится описываемое событие.", font));

			if (person.isChild()) {
				p = new Paragraph("Т.к. прогноз составлен на ребёнка, то следует учесть, "
					+ "что толкования ориентированы на взрослых людей, и их нужно адаптировать к ситуации ребёнка. "
					+ "Например, если в тексте речь идёт о работе, значит имеется в виду учёба; "
					+ "если речь идёт о сотрудничестве, значит имеются в виду ребята из других классов и сообществ и т.п.", red);
				p.setSpacingBefore(10);
				chapter.add(p);				
			}
	        doc.add(chapter);

			Map<Integer, Map<Integer, List<Long>>> years = new TreeMap<Integer, Map<Integer, List<Long>>>();
			Map<Integer, Map<Integer, Map<Long, Integer>>> myears = new TreeMap<Integer, Map<Integer, Map<Long, Integer>>>();
			Map<Integer, Map<Integer, Map<Long, Map<String, List<Object>>>>> texts = new TreeMap<Integer, Map<Integer, Map<Long, Map<String, List<Object>>>>>();
			Map<Integer, Map<Long, Map<Long, TreeMap<Integer, Integer>>>> hyears2 = new TreeMap<Integer, Map<Long, Map<Long, TreeMap<Integer, Integer>>>>();

			Map<Integer, Long[]> categoriesh = new HashMap<Integer, Long[]>() {
				private static final long serialVersionUID = -1169666525576512947L;
				{
			        put(1, new Long[] {144L,145L,146L,147L,148L,149L,153L,156L,157L,159L,161L,162L,165L,166L,167L,170L,171L,174L});
			    }
			};
			Long[] categoryhids = categoriesh.get(category);

			Map<Integer, Long[]> categories = new HashMap<Integer, Long[]>() {
				private static final long serialVersionUID = 7520222202103390519L;
				{
			        put(1, new Long[] {19L,23L,25L,21L,26L,27L,28L,29L,30L,31L,34L});
			    }
			};
			Long[] categorypids = categories.get(category);

			System.out.println("Prepared for: " + (System.currentTimeMillis() - run));
			run = System.currentTimeMillis();

			//разбивка дат по годам и месяцам
			for (Date date = start.getTime(); start.before(end); start.add(Calendar.DATE, 1), date = start.getTime()) {
				Calendar calendar = Calendar.getInstance();
				calendar.setTime(date);
				calendar.set(Calendar.HOUR_OF_DAY, 0);
				calendar.set(Calendar.MINUTE, 0);
				calendar.set(Calendar.SECOND, 0);
				int y = calendar.get(Calendar.YEAR);
				int m = calendar.get(Calendar.MONTH);
				
				Map<Integer, List<Long>> months = years.containsKey(y) ? years.get(y) : new TreeMap<Integer, List<Long>>();
				List<Long> dates = months.containsKey(m) ? months.get(m) : new ArrayList<Long>();
				long time = calendar.getTimeInMillis(); 
				if (!dates.contains(time))
					dates.add(time);
				Collections.sort(dates);
				months.put(m, dates);
				years.put(y, months);

				Map<Integer, Map<Long, Integer>> months2 = myears.containsKey(y) ? myears.get(y) : new TreeMap<Integer, Map<Long, Integer>>();
				months2.put(m, new TreeMap<Long, Integer>());
				myears.put(y, months2);

				Map<Long, Map<Long, TreeMap<Integer, Integer>>> yhouses = hyears2.containsKey(y) ? hyears2.get(y) : new TreeMap<Long, Map<Long, TreeMap<Integer, Integer>>>();
				for (House h : houses.values())
					yhouses.put(h.getId(), new TreeMap<Long, TreeMap<Integer, Integer>>());
				hyears2.put(y, yhouses);
			}

			Map<String, List<DatePeriod>> periods = new HashMap<String, List<DatePeriod>>();
			/**
			 * коды ингрессий, используемых в документе
			 */
			String[] icodes = new String[] {
				Ingress._EXACT, Ingress._EXACT_HOUSE,
				Ingress._SEPARATION, Ingress._SEPARATION_HOUSE,
				Ingress._REPEAT, Ingress._REPEAT_HOUSE
			};

			//создаём аналогичный массив, но с домами вместо дат
			for (Map.Entry<Integer, Map<Integer, List<Long>>> entry : years.entrySet()) {
				int y = entry.getKey();
				Map<Integer, List<Long>> months = years.get(y);
				Map<Integer, Map<Long, Map<String, List<Object>>>> mtexts = texts.get(y);
				if (null == mtexts)
					mtexts = new TreeMap<Integer, Map<Long, Map<String, List<Object>>>>();

				Map<Integer, Map<Long, Integer>> months2 = myears.containsKey(y) ? myears.get(y) : new TreeMap<Integer, Map<Long, Integer>>();
				Map<Long, Map<Long, TreeMap<Integer, Integer>>> yhouses = hyears2.containsKey(y) ? hyears2.get(y) : new TreeMap<Long, Map<Long, TreeMap<Integer, Integer>>>();

				//считаем транзиты
				for (Map.Entry<Integer, List<Long>> entry2 : months.entrySet()) {
					int m = entry2.getKey();

					List<Long> dates = months.get(m);
					Map<Long, Map<String, List<Object>>> dtexts = mtexts.get(m);
					if (null == dtexts)
						dtexts = new TreeMap<Long, Map<String, List<Object>>>();

					//данные для графика месяца
					Map<Long, Integer> seriesh = months2.get(m);

					for (Long time : dates) {
						Date date = new Date(time);
//						System.out.println(date + ", " + finalDate);
//						System.out.println(time + ", " + endtime);
						boolean firstdate = (time == startime);
						boolean lastdate = (time == endtime);
						String sdate = DateUtil.formatCustomDateTime(date, "yyyy-MM-dd") + " 12:00:00";
						Event event = new Event();
						Date edate = DateUtil.getDatabaseDateTime(sdate);
						event.setBirth(edate);
						event.setPlace(new Place().getDefault());
						event.setZone(0);
						event.calc(true);

						Map<String, List<Object>> ingressList = person.initIngresses(event, term);
						if (ingressList.isEmpty())
							continue;

						Map<String, List<Object>> ingressmap = new TreeMap<String, List<Object>>();
						for (Map.Entry<String, List<Object>> daytexts : ingressList.entrySet()) {
							String key = daytexts.getKey();
							if (!Arrays.asList(icodes).contains(key))
								continue;

							List<Object> objects = daytexts.getValue();
							if (objects.isEmpty())
								continue;

							List<Object> objects2 = ingressmap.containsKey(key) ? ingressmap.get(key) : new ArrayList<Object>();
							String[] negatives = {"Kethu", "Lilith"};
							for (Object object : objects) {
								if (object instanceof SkyPointAspect) {
									SkyPointAspect spa = (SkyPointAspect)object;
									Planet planet = (Planet)spa.getSkyPoint1();
									if (planet.getCode().equals("Moon"))
										continue;

//									if (planet.getCode().equals("Mars"))
//										System.out.println(spa);

									if (planet.isMain() && !spa.isRetro())
										continue;

									SkyPoint skyPoint = spa.getSkyPoint2();
									boolean housable = skyPoint instanceof House;
									if (housable) {
		    		                	if (category > 0 && !Arrays.asList(categoryhids).contains(skyPoint.getId()))
		    		                		continue;
		    		                } else {
		    		                	if (category > 0 && !Arrays.asList(categorypids).contains(skyPoint.getId()))
		    		                		continue;
		    		                }									

									String acode = spa.getAspect().getCode();
									if (!acode.equals("CONJUNCTION")) {
										if (planet.isFictitious())
											continue;

										if (!housable && ((Planet)skyPoint).isFictitious())
											continue;
									}
									if (optimistic) {
										if (2 == spa.getAspect().getTypeid())
											continue;

										if (acode.equals("CONJUNCTION")) {
											if (Arrays.asList(negatives).contains(planet.getCode())
													|| Arrays.asList(negatives).contains(skyPoint.getCode()))
												continue;
										}
									}

		    		                if (acode.equals("OPPOSITION")) {
		    		                	if (planet.getCode().equals("Rakhu")
		    		                			|| planet.getCode().equals("Kethu"))
		    		                		continue;
		    		                	if (!housable)
			    		                	if (skyPoint.getCode().equals("Rakhu")
			    		                			|| skyPoint.getCode().equals("Kethu"))
			    		                		continue;
		    		                }
									String code = spa.getCode();
									List<DatePeriod> plist = periods.containsKey(code) ? periods.get(code) : new ArrayList<TransitCycleHandler.DatePeriod>();
									if (key.contains("SEPARATION")
											|| (key.contains("REPEAT") && lastdate)) {
										for (DatePeriod per : plist) {
											if (0 == per.finaldate) {
												per.finaldate = time;
												break;
											}
										}
									} else if (key.contains("EXACT")
											|| (key.contains("REPEAT") && firstdate)) {
										objects2.add(spa);
										DatePeriod period = new DatePeriod();
										period.initdate = time;
										plist.add(period);
										periods.put(code, plist);
									}

									//данные для диаграммы месяца
									if (housable) {
										if (key.contains("EXACT") || key.contains("REPEAT")) {
											//long id = skyPoint.getId();
											String tcode = spa.getAspect().getType().getCode();
											int point = 0;
											if (tcode.equals("NEUTRAL")) {
												if (Arrays.asList(negatives).contains(planet.getCode()))
													--point;
												else
													++point;
											} else if (tcode.equals("POSITIVE"))
												++point;
											else if (tcode.equals("NEGATIVE"))
												--point;

											long houseid = spa.getSkyPoint2().getId();
											//данные для диаграммы сфер жизни
											int val = seriesh.containsKey(houseid) ? seriesh.get(houseid) : 0;
											seriesh.put(houseid, val + point);

											//данные для графиков домов года
											Map<Long, TreeMap<Integer, Integer>> htypes = yhouses.containsKey(houseid) ? yhouses.get(houseid) : new TreeMap<Long, TreeMap<Integer, Integer>>();
											long aid = code.equals("NEUTRAL")
													&& (skyPoint.getCode().equals("Lilith") || skyPoint.getCode().equals("Kethu"))
												? 2 : spa.getAspect().getTypeid();

											TreeMap<Integer, Integer> hmonths = htypes.containsKey(aid) ? htypes.get(aid) : new TreeMap<Integer, Integer>();
											val = hmonths.containsKey(m) ? hmonths.get(m) : 0;
											hmonths.put(m, val + 1);
											htypes.put(aid, hmonths);
											yhouses.put(houseid, htypes);
										}
									}
								}
							}
							ingressmap.put(key, objects2);
						}
						dtexts.put(time, ingressmap);
						mtexts.put(m, dtexts);
						texts.put(y, mtexts);
					}
					months2.put(m, seriesh);
					hyears2.put(y, yhouses);
				}
			}
			years = null;
			System.out.println("Composed for: " + (System.currentTimeMillis() - run));

			//генерируем документ
			run = System.currentTimeMillis();

			DirectionService service = new DirectionService();
			DirectionAspectService servicea = new DirectionAspectService();
			AspectTypeService typeService = new AspectTypeService();
			AspectType positiveType = (AspectType)typeService.find(3L);
			Font hfont = new Font(baseFont, 16, Font.BOLD, PDFUtil.FONTCOLOR);

	        //года
			for (Map.Entry<Integer, Map<Integer, Map<Long, Integer>>> entry : myears.entrySet()) {
				int y = entry.getKey();
				String syear = String.valueOf(y);
				chapter = new ChapterAutoNumber(PDFUtil.printHeader(new Paragraph(), syear + " год", null));
				chapter.setNumberDepth(0);

				//месяцы
				Map<Integer, Map<Long, Integer>> months2 = myears.get(y);
				Map<Integer, Map<Long, Map<String, List<Object>>>> mtexts = texts.get(y);

				for (Map.Entry<Integer, Map<Long, Map<String, List<Object>>>> entry2 : mtexts.entrySet()) {
					int m = entry2.getKey();
					Calendar calendar = Calendar.getInstance();
					calendar.set(y, m, 1);
					String ym = new SimpleDateFormat("LLLL").format(calendar.getTime()) + " " + y;
					Section section = PDFUtil.printSection(chapter, ym, null);

					//диаграмма месяца
					printDiagramDescr(section, font, optimistic);
					Map<Long, Integer> seriesh = months2.get(m);
					Bar[] items = new Bar[seriesh.size()];
					int i = -1;
					for (Map.Entry<Long, Integer> entry3 : seriesh.entrySet()) {
						House house = houses.get(entry3.getKey());
						Bar bar = new Bar();
				    	bar.setName(house.getName());
					    bar.setValue(entry3.getValue());
						bar.setColor(house.getColor());
						bar.setCategory(ym);
						items[++i] = bar;
					}
					section.add(PDFUtil.printBars(writer, "", null, "Сферы жизни", "Баллы", items, 500, 300, false, false, false));
					section.add(new Paragraph("Ниже приведён прогноз по этим сферам жизни", font));
					section.add(Chunk.NEWLINE);

					Map<Long, Map<String, List<Object>>> dtexts = mtexts.get(m);
					if (dtexts.isEmpty())
						continue;
					for (Map.Entry<Long, Map<String, List<Object>>> dentry : dtexts.entrySet()) {
						Map<String, List<Object>> imap = dentry.getValue();
						boolean empty = true;
						for (Map.Entry<String, List<Object>> daytexts : imap.entrySet()) {
							List<Object> ingresses = daytexts.getValue();
							if (!ingresses.isEmpty()) {
								empty = false;
								break;
							}
						}
						if (empty) continue;

						String shortdate = sdf.format(new Date(dentry.getKey()));
						Section daysection = PDFUtil.printSubsection(section, shortdate, null);

						for (Map.Entry<String, List<Object>> itexts : imap.entrySet()) {
							List<Object> ingresses = itexts.getValue();
							for (Object object : ingresses) {
								text = "";

								if (object instanceof SkyPointAspect) {
									SkyPointAspect spa = (SkyPointAspect)object;
									Planet planet = (Planet)spa.getSkyPoint1();
		    		                boolean repeat = itexts.getKey().contains("REPEAT");
									SkyPoint skyPoint = spa.getSkyPoint2();
									String acode = spa.getAspect().getCode();

									String prefix = "";
									if (!planet.isFictitious())
										prefix = repeat ? "Продолжается: " : "Начинается: ";
									AspectType type = spa.getAspect().getType();
									String typeColor = type.getFontColor();
									BaseColor color = PDFUtil.htmlColor2Base(typeColor);
									Font colorbold = new Font(baseFont, 12, Font.BOLD, color);

									String til = "";
									List<DatePeriod> plist = periods.get(spa.getCode());
									if (plist != null && !plist.isEmpty()) {
										for (DatePeriod per : plist) {
											long time = dentry.getKey();
											if (per.finaldate > 0
													&& time == per.initdate) {
												Date pdate = new Date(per.finaldate);
												til = " (до " + spf.format(pdate) + " " + y + ")";
												break;
											}
										}
									}

									if (skyPoint instanceof House) {
										House house = (House)skyPoint;
	
			    		                if (acode.equals("CONJUNCTION"))
											if (planet.getCode().equals("Selena"))
												type = positiveType;
	
										DirectionText dirText = (DirectionText)service.find(planet, house, type);
										if (dirText != null)
											text = spa.isRetro() && !planet.isFictitious() ? dirText.getRetro() : dirText.getDescription();
										String ptext = prefix;
										if (null == dirText)
											ptext += planet.getShortName() + " " + type.getSymbol() + " " + house.getName() + "<>";
										else
											ptext += term ? planet.getName() + " " + type.getSymbol() + " " + house.getDesignation() + " дом" : house.getName();

					    				daysection.addSection(new Paragraph(ptext + til, colorbold));

									} else if (skyPoint instanceof Planet) {
										long aspectid = 0;
										boolean checktype = false;
										Planet planet2 = (Planet)skyPoint;
										boolean revolution = planet.getId().equals(planet2.getId());

										PlanetAspectText dirText = (PlanetAspectText)servicea.find(spa, aspectid, checktype);
										if (dirText != null)
											text = dirText.getDescription();
										String ptext = prefix;
										if (null == dirText) {
											ptext += planet.getShortName() + "<>";
											if (!revolution)
												ptext += " " + type.getSymbol() + " " + planet2.getShortName() + "<>";
										} else {
											ptext += term ? planet.getName() : planet.getShortName();
											if (!revolution)
												ptext += " " + type.getSymbol() + " " + (term ? planet2.getName() : planet2.getShortName());
										}
										daysection.addSection(new Paragraph(ptext + til, colorbold));
									}
	
									if (text != null) {
										String descr = text;
										p = new Paragraph();
										p.add(new Chunk(descr, new Font(baseFont, 12, Font.NORMAL, color)));
										daysection.add(p);
									}
								}
								daysection.add(Chunk.NEWLINE);
							}
						}
					}
//					texts = null;
			        chapter.add(Chunk.NEXTPAGE);
				}
				//графики года
				Section section = PDFUtil.printSection(chapter, "Диаграммы сфер жизни по месяцам " + y + " года", "charts" + y);
				Map<Long, Map<Long, TreeMap<Integer, Integer>>> yhouses = hyears2.get(y);
				int i = -1;
				for (Map.Entry<Long, Map<Long, TreeMap<Integer, Integer>>> entryh : yhouses.entrySet()) {
					long houseid = entryh.getKey();
					House house = houses.get(houseid);
					Map<Long, TreeMap<Integer, Integer>> mtypes = entryh.getValue();
					if (mtypes.isEmpty())
						continue;
					Map<String, Object[]> types = new HashMap<String, Object[]>();
		        	for (Map.Entry<Long, TreeMap<Integer, Integer>> entrya : mtypes.entrySet()) {
		        		TreeMap<Integer, Integer> atypes = entrya.getValue();
						if (atypes.isEmpty())
							continue;

						Long aid = entrya.getKey();
						List<Integer> names = new ArrayList<Integer>();
						List<Integer> values = new ArrayList<Integer>();
			        	for (Map.Entry<Integer, Integer> entrym : atypes.entrySet()) {
			        		int m = entrym.getKey();
			        		names.add(m + 1);
			        		values.add(entrym.getValue());
			        	}
						String name = (aid < 2 ? "Важное" : (aid < 3 ? "Негатив" : "Позитив"));
			        	types.put(name, new Object[] {names, values});
		        	}
		        	if (types.isEmpty())
		        		continue;
					section.addSection(new Paragraph(house.getName(), hfont));
		        	section.add(new Paragraph(y + ": " + house.getDescription(), font));
				    Image image = PDFUtil.printGraphics(writer, "", "Месяцы " + y, "Баллы", types , 500, 0, true);
					section.add(image);
					if (++i % 2 > 0)
						section.add(Chunk.NEXTPAGE);
					else
						section.add(Chunk.NEWLINE);
				}
				doc.add(chapter);
			}
			myears = null;
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

	private class DatePeriod {
		long initdate;
		long finaldate;

		@Override
		public String toString() {
			return initdate + " - " + finaldate;
		}
	}

	/**
	 * Выводим описание диаграммы возраста
	 * @param section раздел
	 * @param font шрифт
	 */
	private void printDiagramDescr(Section section, Font font, boolean optimistic) {
		if (OsUtil.getOS().equals(OsUtil.OS.LINUX)) {
			String text = "Диаграмма показывает, какие сферы жизни будут актуальны в течение месяца:";
			section.add(new Paragraph(text, font));
	
			com.itextpdf.text.List list = new com.itextpdf.text.List(false, false, 10);
			ListItem li = new ListItem();
	        li.add(new Chunk("Показатели выше нуля указывают на успех и лёгкость", new Font(baseFont, 12, Font.NORMAL, new BaseColor(0, 102, 102))));
	        list.add(li);

			li = new ListItem();
	        li.add(new Chunk("Показатели на нуле указывают на нейтральность ситуации", font));
	        list.add(li);
	
	        if (!optimistic) {
				li = new ListItem();
		        li.add(new Chunk("Показатели ниже нуля указывают на трудности и напряжение", new Font(baseFont, 12, Font.NORMAL, new BaseColor(102, 0, 51))));
		        list.add(li);
	        }
	        section.add(list);
		} else {
			String text = "Диаграмма показывает, какие сферы жизни будут актуальны в течение месяца:";
			section.add(new Paragraph(text, font));			
		}
	}
}
