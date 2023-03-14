package kz.zvezdochet.direction.exporter;

import java.io.FileOutputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import com.itextpdf.text.Anchor;
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

import kz.zvezdochet.analytics.bean.Rule;
import kz.zvezdochet.analytics.exporter.EventRules;
import kz.zvezdochet.bean.Aspect;
import kz.zvezdochet.bean.AspectType;
import kz.zvezdochet.bean.Event;
import kz.zvezdochet.bean.House;
import kz.zvezdochet.bean.Planet;
import kz.zvezdochet.bean.PositionType;
import kz.zvezdochet.bean.Sign;
import kz.zvezdochet.bean.SkyPoint;
import kz.zvezdochet.bean.SkyPointAspect;
import kz.zvezdochet.core.bean.Model;
import kz.zvezdochet.core.service.DataAccessException;
import kz.zvezdochet.core.util.CalcUtil;
import kz.zvezdochet.core.util.CoreUtil;
import kz.zvezdochet.core.util.OsUtil;
import kz.zvezdochet.core.util.PlatformUtil;
import kz.zvezdochet.direction.Activator;
import kz.zvezdochet.direction.bean.DirectionAspectText;
import kz.zvezdochet.direction.bean.DirectionRule;
import kz.zvezdochet.direction.bean.DirectionText;
import kz.zvezdochet.direction.service.DirectionAspectService;
import kz.zvezdochet.direction.service.DirectionRuleService;
import kz.zvezdochet.direction.service.DirectionService;
import kz.zvezdochet.export.bean.Bar;
import kz.zvezdochet.export.handler.PageEventHandler;
import kz.zvezdochet.export.util.PDFUtil;
import kz.zvezdochet.service.HouseService;
import kz.zvezdochet.service.PlanetService;
import kz.zvezdochet.service.PositionTypeService;
import kz.zvezdochet.util.HouseMap;

/**
 * Генератор PDF-файла для экспорта событий
 * @author Natalie Didenko
 *
 */
public class PDFExporter {
	/**
	 * Базовый шрифт
	 */
	private BaseFont baseFont;
	/**
	 * Вариации шрифтов
	 */
	private Font font, fonth5;
	/**
	 * Признак использования астрологических терминов
	 */
	private boolean term = false;
	/**
	 * Признак оптимистичного прогноза
	 */
	private boolean optimistic = false;
	/**
	 * Язык файла
	 */
	private String lang = "ru";

	public PDFExporter() {
		try {
			baseFont = PDFUtil.getBaseFont();
			font = PDFUtil.getRegularFont();
			fonth5 = PDFUtil.getHeaderFont();
			lang = Locale.getDefault().getLanguage();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Генерация событий периода
	 * @param event событие
	 * @param spas список аспектов планет и домов
	 * @param initage начальный возраст
	 * @param years количество лет
	 * @param optimistic 0|1 реалистичный|оптимистичный
	 * @param term 0|1 без терминов|с терминами
	 */
	public void generate(Event event, List<SkyPointAspect> spas, int initage, int years, boolean optimistic, boolean term) {
		this.optimistic = optimistic;
		this.term = term;
		Document doc = new Document();
		try {
			String filename = PlatformUtil.getPath(Activator.PLUGIN_ID, "/out/longterm.pdf").getPath();
			PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(filename));
			PageEventHandler handler = new PageEventHandler();
			handler.setLang(lang);
	        writer.setPageEvent(handler);
	        doc.open();

			String lang = Locale.getDefault().getLanguage();
			boolean rus = lang.equals("ru");

	        //metadata
	        PDFUtil.getMetaData(doc, "Долгосрочный прогноз", lang);

	        //раздел
			Chapter chapter = new ChapterAutoNumber(PDFUtil.printHeader(new Paragraph(), "Долгосрочный прогноз", null));
			chapter.setNumberDepth(0);

			//шапка
			String text = (event.isCelebrity() ? event.getName(lang) : event.getCallname(lang));
			text += " - прогноз на " + CoreUtil.getAgeString(years);
			Paragraph p = new Paragraph(text, font);
			if (!event.isRectified())
				p.add(new Chunk(" (не ректифицировано)", PDFUtil.getDangerFont()));
	        p.setAlignment(Element.ALIGN_CENTER);
			chapter.add(p);

			p = new Paragraph("Тип прогноза: " + (optimistic ? "оптимистичный" : "реалистичный"), font);
	        p.setAlignment(Element.ALIGN_CENTER);
			chapter.add(p);

			Font fontgray = PDFUtil.getAnnotationFont(false);
			text = kz.zvezdochet.core.Messages.getString("Created at") + ": " +
				DateFormat.getDateInstance(DateFormat.FULL, Locale.getDefault()).format(event.isCelebrity() ? event.getDate() : new Date());
			p = new Paragraph(text, fontgray);
	        p.setAlignment(Element.ALIGN_CENTER);
			chapter.add(p);

			p = new Paragraph();
	        p.setAlignment(Element.ALIGN_CENTER);
			p.setSpacingAfter(20);
	        p.add(new Chunk("Автор: ", fontgray));
	        Chunk chunk = new Chunk(PDFUtil.getAuthor(lang), new Font(baseFont, 10, Font.UNDERLINE, PDFUtil.FONTCOLOR));
	        chunk.setAnchor(PDFUtil.getWebsite(lang));
	        p.add(chunk);
	        chapter.add(p);

	        boolean chartable = years > 4;
	        chapter.add(new Paragraph("Данный прогноз не содержит конкретных дат, "
	        	+ "но описывает самые значительные тенденции вашей жизни в ближайшие " + CoreUtil.getAgeString(years)
        		+ " независимо от переездов и местоположения.", font));
			Font red = PDFUtil.getDangerFont();
			String months = event.isRectified() ? "6 месяцев" : "1 год";
			chapter.add(new Paragraph("Максимальная погрешность прогноза ±" + months + ".", red));

			p = new Paragraph("Если в прогнозе упомянуты люди, которых уже нет в живых (родители, супруги, родственники), "
				+ "значит речь идёт о людях, их заменяющих (опекуны, крёстные) или похожих на них по характеру.", font);
			p.setSpacingBefore(10);
			chapter.add(p);

			if (event.isChild()) {
				p = new Paragraph("Т.к. прогноз составлен на ребёнка, то следует учесть, "
					+ "что толкования ориентированы на взрослых людей, и их нужно адаптировать к ситуации ребёнка. "
					+ "Например, если в тексте речь идёт о работе, значит имеется в виду учёба; "
					+ "если речь идёт о сотрудничестве, значит имеются в виду ребята из других классов и сообществ и т.п.", red);
				p.setSpacingBefore(10);
				chapter.add(p);				
			}

			//данные для графика
			Map<Integer,Integer> positive = new HashMap<Integer,Integer>();
			Map<Integer,Integer> negative = new HashMap<Integer,Integer>();

			for (int i = 0; i < years; i++) {
				int nextage = initage + i;
				positive.put(nextage, 0);
				negative.put(nextage, 0);
			}
			int finalage = initage + years - 1;

			Map<Long, Map<Integer, Double>> seriesh = new HashMap<Long, Map<Integer, Double>>();
			Map<Integer, Map<Long, Double>> seriesa = new HashMap<Integer, Map<Long, Double>>();

			//события
			Map<Integer, TreeMap<Integer, List<SkyPointAspect>>> map = new HashMap<Integer, TreeMap<Integer, List<SkyPointAspect>>>();
			for (SkyPointAspect spa : spas) {
				int age = (int)spa.getAge();
				if (age > finalage)
					continue;

				Planet planet = (Planet)spa.getSkyPoint1();
				String pcode = planet.getCode();
				boolean isHouse = spa.getSkyPoint2() instanceof House;

				if (spa.getAspect().getCode().equals("OPPOSITION")) {
					if (isHouse) {
						if	(pcode.equals("Kethu") || pcode.equals("Rakhu"))
							continue;
					} else {
						Planet planet2 = (Planet)spa.getSkyPoint2();
						String pcode2 = planet2.getCode();
						if	(pcode.equals("Kethu") || pcode.equals("Rakhu")
								|| pcode2.equals("Kethu") || pcode2.equals("Rakhu"))
							continue;
					}
				}

				TreeMap<Integer, List<SkyPointAspect>> agemap = map.get(age);
				if (null == agemap) {
					agemap = new TreeMap<Integer, List<SkyPointAspect>>();
					agemap.put(0, new ArrayList<SkyPointAspect>());
					agemap.put(1, new ArrayList<SkyPointAspect>());
					agemap.put(2, new ArrayList<SkyPointAspect>());
				}

				String code = spa.getAspect().getType().getCode();
				if (code.equals("NEUTRAL") || code.equals("NEGATIVE") || code.equals("POSITIVE")) {
					if (isHouse) {
						if (code.equals("NEUTRAL")) {
							List<SkyPointAspect> list = agemap.get(0);
							list.add(spa);
						} else {
							List<SkyPointAspect> list = agemap.get(1);
							list.add(spa);
						}
					} else {
						List<SkyPointAspect> list = agemap.get(2);
						list.add(spa);
					}
					double point = 0;
					if (code.equals("NEUTRAL")) {
						if (pcode.equals("Lilith") || pcode.equals("Kethu")) {
							negative.put(age, negative.get(age) + 1);
							--point;
						} else {
							positive.put(age, positive.get(age) + 1);
							++point;
						}
					} else if (code.equals("POSITIVE")) {
						positive.put(age, positive.get(age) + 1);
						point += 2;
					} else if (code.equals("NEGATIVE")) {
						negative.put(age, negative.get(age) + 1);
						--point;
					}

					if (isHouse) {
						long houseid = spa.getSkyPoint2().getId();
						//данные для диаграммы сфер жизни
						Map<Integer, Double> submap = seriesh.containsKey(houseid) ? seriesh.get(houseid) : new HashMap<Integer, Double>();
						double val = submap.containsKey(age) ? submap.get(age) : 0;
						submap.put(age, val + point);
						seriesh.put(houseid, submap);

						//данные для диаграммы возраста
						Map<Long, Double> submapa = seriesa.containsKey(age) ? seriesa.get(age) : new HashMap<Long, Double>();
						val = submapa.containsKey(houseid) ? submapa.get(houseid) : 0;
						submapa.put(houseid, val + point);
						seriesa.put(age, submapa);
					}
				}
				map.put(age, agemap);
			}

			Bar[] bars = new Bar[years * 2];
			for (int i = 0; i < years; i++) {
				int nextage = initage + i;
				String strage = CoreUtil.getAgeString(nextage);
				bars[i] = new Bar(strage, positive.get(nextage), null, "Позитивные события", null);
				bars[i + years] = new Bar(strage, negative.get(nextage) * (-1), null, "Негативные события", null);
			}
			int height = 400;
			if (years < 2)
				height = 140;
			else if (years < 4)
				height = 200;
			Image image = PDFUtil.printStackChart(writer, "Соотношение категорий событий", "Возраст", "Количество", bars, 500, height, true);
			chapter.add(image);
			if (years > 2)
				chapter.add(Chunk.NEXTPAGE);
			else
				chapter.add(Chunk.NEWLINE);

			Font bold = new Font(baseFont, 12, Font.BOLD);
			chapter.add(new Paragraph("Примечание:", bold));
			com.itextpdf.text.List list = new com.itextpdf.text.List(false, false, 10);
	        ListItem li = new ListItem();
	        li.add(new Chunk("Чёрным цветом выделены важные тенденции, которые указывают на основополагающие события периода, - это самое важное, что с вами произойдёт. "
	        	+ "Эти тенденции могут сохраняться в течение двух лет. А всё остальное продлится не более трёх месяцев.", font));
	        list.add(li);

			Font green = PDFUtil.getSuccessFont();
			li = new ListItem();
	        li.add(new Chunk("Зелёным цветом выделены позитивные тенденции. "
	        	+ "К ним относятся события, которые сами по себе удачно складываются " 
	        	+ "и представляют собой благоприятные возможности, наполняющие вас энергией. Их надо использовать по максимуму.", green));
	        list.add(li);

	        if (!optimistic) {
				li = new ListItem();
		        li.add(new Chunk("Красным цветом выделены негативные тенденции, которые потребуют большого расхода энергии. "
		        	+ "Они указывают на сферы, от которых не нужно ждать многого. "
		        	+ "Это признак того, что вам необходим отдых, переосмысление и мобилизация ресурсов для решения проблемы. "
					+ "А также это возможность смягчить напряжение, ведь вы будете знать о нём заранее. "
					+ "Не зацикливайтесь на негативе, используйте по максимуму свои сильные стороны и благоприятные события прогноза.", red));
		        list.add(li);
	        }

			li = new ListItem();
	        li.add(new Chunk("В течение года в одной и той же сфере жизни могут происходить как напряжённые, так и приятные события.", font));
	        list.add(li);

	        if (years > 1) {
				li = new ListItem();
		        li.add(new Chunk("Если из возраста в возраст событие повторяется, значит оно создаст большой резонанс.", font));
		        list.add(li);
	        }
	        chapter.add(list);

	        chapter.add(Chunk.NEWLINE);
	        chapter.add(new Paragraph("Заголовки абзацев (например, «Добро + Прибыль») используются для структурирования текста и "
	        	+ "указывают на сферу жизни, к которой относится описываемое событие.", font));

			chapter.add(Chunk.NEWLINE);
			p = new Paragraph("Самые важные события периода", fonth5);
			p.setSpacingAfter(10);
			chapter.add(p);
			p = new Paragraph("Чтобы увидеть самые важные моменты ближайших лет, "
				+ "почитайте раздел «Значимые события» каждого года.", font);
			chapter.add(p);

	        //инструкция по поиску события
	        if (chartable) {
				chapter.add(Chunk.NEWLINE);
				p = new Paragraph("Как найти конкретное событие?", fonth5);
				p.setSpacingAfter(10);
				chapter.add(p);
				com.itextpdf.text.List ilist = new com.itextpdf.text.List(false, false, 10);
				li = new ListItem();
				Font fonta = PDFUtil.getLinkFont();
				Anchor anchor = new Anchor("Диаграммы", fonta);
	            anchor.setReference("#diagrams");
				li.add(new Chunk("Перейдите в раздел ", font));
		        li.add(anchor);
		        li.add(new Chunk(" в конце документа;", font));
		        ilist.add(li);

				li = new ListItem();
		        li.add(new Chunk("найдите диаграмму нужной вам категории событий;", font));
		        ilist.add(li);

				li = new ListItem();
		        li.add(new Chunk("посмотрите возраст на вершине графика (жирная точка). Указанное событие должно произойти в этом возрасте;", font));
		        ilist.add(li);
	
				li = new ListItem();
		        li.add(new Chunk("перейдите в раздел данного возраста и прочтите толкование события", font));
		        ilist.add(li);
		        chapter.add(ilist);
	        }
	        doc.add(chapter);

			HouseService serviceh = new HouseService();
			Map<Integer, TreeMap<Integer, List<SkyPointAspect>>> treeMap = new TreeMap<Integer, TreeMap<Integer, List<SkyPointAspect>>>(map);
			for (Map.Entry<Integer, TreeMap<Integer, List<SkyPointAspect>>> entry : treeMap.entrySet()) {
				TreeMap<Integer, List<SkyPointAspect>> agemap = entry.getValue();
				if (agemap.isEmpty())
					continue;

			    int age = entry.getKey();
			    String agestr = CoreUtil.getAgeString(age);
				chapter = new ChapterAutoNumber(PDFUtil.printHeader(new Paragraph(), agestr, null));
				chapter.setNumberDepth(0);

				//диаграмма возраста
				Section section = PDFUtil.printSection(chapter, "Диаграмма " + agestr, null);
				printDiagramDescr(section);
				Map<Long, Double> mapa = seriesa.get(age);
				Bar[] items = new Bar[mapa.size()];
				int i = -1;
				for (Map.Entry<Long, Double> entry2 : mapa.entrySet()) {
					House house = (House)serviceh.find(entry2.getKey());
					Bar bar = new Bar();
			    	bar.setName(house.getName());
				    bar.setValue(entry2.getValue());
					bar.setColor(house.getColor());
					bar.setCategory(age + "");
					items[++i] = bar;
				}
				section.add(PDFUtil.printBars(writer, "", null, "Сферы жизни", "Баллы", items, 500, 300, false, false, false));
				section.add(new Paragraph("Ниже приведён прогноз по этим сферам жизни", font));
				chapter.add(Chunk.NEXTPAGE);

				for (Map.Entry<Integer, List<SkyPointAspect>> subentry : agemap.entrySet())
					printEvents(event, chapter, age, subentry.getKey(), subentry.getValue(), agemap.get(2), rus);
				doc.add(chapter);
				doc.add(Chunk.NEXTPAGE);
			}
			
//			if (term) {
//				chapter = new ChapterAutoNumber(PDFUtil.printHeader(new Paragraph(), "Сокращения", null));
//				chapter.setNumberDepth(0);
//
//				chapter.add(new Paragraph("Раздел событий:", font));
//				list = new com.itextpdf.text.List(false, false, 10);
//				li = new ListItem();
//		        li.add(new Chunk("\u2191 — сильная планета, адекватно проявляющая себя в астрологическом доме", font));
//		        list.add(li);
//
//				li = new ListItem();
//		        li.add(new Chunk("\u2193 — ослабленная планета, источник неуверенности, стресса и препятствий", font));
//		        list.add(li);
//
//				li = new ListItem();
//		        li.add(new Chunk("обт — указанный астрологический дом является обителью планеты и облегчает ей естественное и свободное проявление", font));
//		        list.add(li);
//
//				li = new ListItem();
//		        li.add(new Chunk("экз — указанный астрологический дом является местом экзальтации планеты, усиливая её проявления и уравновешивая слабые качества", font));
//		        list.add(li);
//
//				li = new ListItem();
//		        li.add(new Chunk("пдн — указанный астрологический дом является местом падения планеты, где она чувствует себя «не в своей тарелке»", font));
//		        list.add(li);
//
//				li = new ListItem();
//		        li.add(new Chunk("изг — указанный астрологический дом является местом изгнания планеты, ослабляя её проявления и усиливает негатив", font));
//		        list.add(li);
//		        chapter.add(list);
//
//		        chapter.add(Chunk.NEWLINE);
//				chapter.add(new Paragraph("Раздел личности:", font));
//				list = new com.itextpdf.text.List(false, false, 10);
//				li = new ListItem();
//		        li.add(new Chunk("\u2191 — усиленный аспект, проявляющийся ярче других аспектов указанных планет (хорошо для позитивных сочетаний, плохо для негативных)", font));
//		        list.add(li);
//
//				li = new ListItem();
//		        li.add(new Chunk("\u2193 — ослабленный аспект, проявляющийся менее ярко по сравнению с другими аспектами указанных планет (плохо для позитивных сочетаний, хорошо для негативных)", font));
//		        list.add(li);
//		        chapter.add(list);
//				doc.add(chapter);
//			}

	        if (chartable) {
				chapter = new ChapterAutoNumber(PDFUtil.printHeader(new Paragraph(), "Диаграммы", null));
				chapter.setNumberDepth(0);
	
				p = new Paragraph();
				text = "Диаграммы обобщают приведённую выше информацию и наглядно отображают сферы жизни, которые будут занимать вас в каждом конкретном возрасте:";
				Anchor anchorTarget = new Anchor(text, font);
				anchorTarget.setName("diagrams");
			    p.add(anchorTarget);
				chapter.add(p);
	
				list = new com.itextpdf.text.List(false, false, 10);
				li = new ListItem();
		        li.add(new Chunk("Показатели выше нуля указывают на успех и лёгкость", new Font(baseFont, 12, Font.NORMAL, new BaseColor(0, 102, 102))));
		        list.add(li);
	
				li = new ListItem();
		        li.add(new Chunk("Показатели на нуле указывают на нейтральность ситуации", font));
		        list.add(li);
	
				li = new ListItem();
		        li.add(new Chunk("Показатели ниже нуля указывают на трудности и напряжение", new Font(baseFont, 12, Font.NORMAL, new BaseColor(102, 0, 51))));
		        list.add(li);
		        chapter.add(list);
		        chapter.add(Chunk.NEWLINE);
	
				p = new Paragraph("Если график представляет собой точку, значит актуальность данной сферы жизни будет ограничена одним годом. " +
					"Если график изображён в виде линии, значит в течение нескольких лет произойдёт череда событий в данной сфере", font);
				chapter.add(p);

		        HouseMap[] houseMap = HouseMap.getMap();
		        for (HouseMap hmap : houseMap) {
		        	Section section = PDFUtil.printSection(chapter, hmap.name, null);
			        Map<String, Object[]> smap = new HashMap<>();
					List<String> descrs = new ArrayList<String>();
		        	for (int i = 0; i < 3; i++) {
		        		long houseid = hmap.houseids[i];
		        		Map<Integer, Double> hdata = seriesh.get(houseid);
						House house = (House)serviceh.find(houseid);
		        		if (null == hdata || 0 == hdata.size()) {
	//	        			if (i > 0) {
	//							series.add(initage, 0);
	//					        items.addSeries(series);
	//	        			}
		        			continue;
		        		}
						descrs.add(house.getName() + ": " + house.getDescription());

						List<Integer> names = new ArrayList<Integer>();
						List<Double> values = new ArrayList<Double>();
		        		
		        		SortedSet<Integer> keys = new TreeSet<Integer>(hdata.keySet());
		        		for (Integer key : keys) {
							names.add(key);
							values.add(hdata.get(key));
						}
		        		smap.put(house.getName(), new Object[] {names, values});
		        	}	        	
					image = PDFUtil.printGraphics(writer, "", "Возраст", "Баллы", smap, 500, 300, true);
					section.add(image);
	
					list = new com.itextpdf.text.List(false, false, 10);
					for (String descr : descrs) {
						li = new ListItem();
						li.add(new Chunk(descr, font));
						list.add(li);
					}
					section.add(list);
					chapter.add(Chunk.NEXTPAGE);
		        }
				doc.add(chapter);
	        }
	        doc.add(PDFUtil.printCopyright(lang));
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
	        doc.close();
		}
	}

	/**
	 * Генерация событий по категориям
	 * @param event персона
	 * @param chapter раздел документа
	 * @param age возраст
	 * @param code код подраздела
	 * @param spas список событий
	 * @param spas2 список аспектов дирекционных планет возраста
	 * @param rus true - русский язык
	 */
	private Section printEvents(Event event, Chapter chapter, int age, int code, List<SkyPointAspect> spas, List<SkyPointAspect> spas2, boolean rus) {
		try {
			if (spas.isEmpty())
				return null;

			Font grayfont = PDFUtil.getAnnotationFont(false);
			Font red = PDFUtil.getDangerFont();
			Font orange = PDFUtil.getWarningFont();

			String header = "";
			Paragraph p = null;
			String agestr = CoreUtil.getAgeString(age);
			if (0 == code) {
				header += "Значимые события " + agestr;
				p = new Paragraph("В данном разделе описаны жизненноважные, долгожданные, переломные события, "
					+ "которые произойдут в возрасте " + agestr + ", надолго запомнятся и повлекут за собой перемены", grayfont);
			} else if (1 == code) {
				header += "Менее значимые события " + agestr;
				p = new Paragraph("В данном разделе описаны краткосрочные, но важные события, "
					+ "которые произойдут в возрасте " + agestr + " и не будут иметь последствий дольше одного года", grayfont);
			} else if (2 == code) {
				header += "Проявления личности в " + agestr;
				p = new Paragraph("В данном разделе описаны черты вашей личности, которые станут особенно яркими в возрасте " + agestr, grayfont);
			}
			Section section = PDFUtil.printSection(chapter, header, null);
			if (p != null) {
				p.setSpacingAfter(10);
				section.add(p);
			}
			boolean female = event.isFemale();

			DirectionService service = new DirectionService();
			DirectionAspectService servicea = new DirectionAspectService();
			DirectionRuleService servicer = new DirectionRuleService();

			boolean child = event.isChild();
			String[] adult = {"II_3", "V_2", "V_3", "VII"};
			String[] pnegative = {"Lilith", "Kethu"};

			Font fonth6 = PDFUtil.getSubheaderFont();
			Font afont = PDFUtil.getHeaderAstroFont();

			for (SkyPointAspect spa : spas) {
				AspectType type = spa.getAspect().getType();
				if (type.getCode().contains("HIDDEN"))
					continue;

				if (optimistic && 2 == spa.getAspect().getTypeid())
					continue;

				Planet planet = (Planet)spa.getSkyPoint1();
				SkyPoint skyPoint = spa.getSkyPoint2();
				if (optimistic
						&& 1 == spa.getAspect().getTypeid()
						&& (Arrays.asList(pnegative).contains(planet.getCode())
							|| Arrays.asList(pnegative).contains(skyPoint.getCode())))
					continue;

				String acode = spa.getAspect().getCode();
				boolean conj = acode.equals("CONJUNCTION");

				if (skyPoint instanceof House) {
					if (child && Arrays.asList(adult).contains(skyPoint.getCode()))
						continue;

					House house = (House)skyPoint;
					List<Model> dirTexts = service.finds(planet, house, type);
					for (Model model : dirTexts) {
						DirectionText dirText = (DirectionText)model;
						Aspect diraspect = dirText.getAspect();
						boolean aspectable = (diraspect != null);
						if (aspectable && !diraspect.getId().equals(spa.getAspect().getId()))
							continue;

						boolean negative = (null == dirText) ? type.getPoints() < 0 : !dirText.isPositive();
						if (!aspectable) {
		    				String pname = negative ? event.getPlanets().get(planet.getId()).getBadName() : event.getPlanets().get(planet.getId()).getGoodName();
							String text = house.getName() + " " + type.getSymbol() + " " + pname;
							section.addSection(new Paragraph(text, fonth5));
							if (term) {
								String pretext = acode.equals("CONJUNCTION")
									? (null == house.getGeneral() ? "с куспидом" : "с вершиной")
									: (null == house.getGeneral() ? "к куспиду" : "к вершине");
		
								p = new Paragraph();
					    		p.add(new Chunk(spa.getAspect().getName() + " дирекционной планеты ", grayfont));
			    				p.add(new Chunk(planet.getSymbol(), afont));
			    				p.add(new Chunk(" " + planet.getName(), grayfont));
								p.add(new Chunk(" из " + CalcUtil.roundTo(planet.getLongitude(), 2) + "° (", grayfont));
								Sign sign = planet.getSign();
			    				p.add(new Chunk(sign.getSymbol(), afont));
			    				p.add(new Chunk(" " + sign.getName(), grayfont));
			    				String mark = planet.getMark("sign", term, lang);
			    				p.add(new Chunk((mark.isEmpty() ? "" : " " + mark) + ", ", grayfont));
			    				House house2 = planet.getHouse();
								p.add(new Chunk(house2.getDesignation() + " дом, сектор «" + house2.getName() + "»", grayfont));
								mark = planet.getMark("house", term, lang);
			    				p.add(new Chunk((mark.isEmpty() ? "" : " " + mark) + ") ", grayfont));
								p.add(new Chunk(pretext + " " + house.getDesignation() + " дома", grayfont));
			    				if (!conj)
									p.add(new Chunk(" (сектор «" + house.getName() + "»)", grayfont));
			    				section.add(p);
			    				if (conj)
			    					section.add(Chunk.NEWLINE);
							}
						}	
						if (dirText != null) {
							if (!aspectable) {
								if (acode.equals("QUADRATURE"))
									section.add(new Paragraph("Уровень критичности: высокий", red));
								else if (acode.equals("OPPOSITION"))
									section.add(new Paragraph("Уровень критичности: средний", red));
								else if (acode.equals("TRIN"))
									section.add(new Paragraph("Уровень успеха: высокий", orange));
								else if (acode.equals("SEXTILE"))
									section.add(new Paragraph("Уровень успеха: средний", orange));
							}	
							String text = dirText.getText();
							if (text != null) {
								String typeColor = type.getFontColor();
								BaseColor color = PDFUtil.htmlColor2Base(typeColor);
								section.add(new Paragraph(PDFUtil.removeTags(text, new Font(baseFont, 12, Font.NORMAL, color))));
	
								//правила домов
								List<DirectionRule> houseRules = servicer.findRules(planet, house, spa.getAspect().getType());
								for (DirectionRule rule : houseRules) {
									AspectType aspectType = rule.getAspectType();
									Aspect aspect = rule.getAspect();
									Planet planet2 = rule.getPlanet2();
									House house2 = rule.getHouse2();

									//дирекционные аспекты планет между собой
									for (SkyPointAspect spa2 : spas2) {
										if (aspect != null
												&& !aspect.getId().equals(spa2.getAspect().getId()))
											continue;
	
										SkyPoint sp = spa2.getSkyPoint2();
										if (aspectType.getId().equals(spa2.getAspect().getTypeid())) {
											if (planet2.getId().equals(sp.getId())) {
												if (house2 != null
														&& !house2.getId().equals(sp.getHouse().getId()))
													continue;
	
												section.add(Chunk.NEWLINE);
												boolean negative2 = spa2.isNegative();
												String sign2 = negative2 ? "-" : "+";
												String header2 = rule.getHouse().getName() + " " + 
													sign2 + " " + 
													(negative2 ? planet2.getNegative() : planet2.getPositive());
												section.add(new Paragraph(header2, fonth6));
												if (term) {
													p = new Paragraph();
										    		p.add(new Chunk(spa2.getAspect().getName() + " дирекционной планеты", grayfont));
								    				p.add(new Chunk(" " + planet.getName(), grayfont));
								    				p.add(new Chunk(" к натальной планете " + planet2.getName(), grayfont));
										    		p.add(new Chunk(", находящейся в " + house2.getDesignation() + " доме (сектор «" + house2.getName() + "»)", grayfont));
								    				section.add(p);
												}
												section.add(new Paragraph(PDFUtil.removeTags(rule.getText(), font)));
												//PDFUtil.printGender(section, rule, female, child, true);
											}
										}
									}

									//дирекционные аспекты планет с домами
									for (SkyPointAspect spa2 : spas) {
										SkyPoint sp = spa2.getSkyPoint2();
										if (sp instanceof Planet)
											continue;

										if (aspect != null
												&& !aspect.getId().equals(spa2.getAspect().getId()))
											continue;
	
										if (aspectType.getId().equals(spa2.getAspect().getTypeid())) {
											SkyPoint sp1 = spa2.getSkyPoint1();
											if (planet2.getId().equals(sp1.getId())) {
												if (!house2.getId().equals(sp.getId()))
													continue;
	
												section.add(Chunk.NEWLINE);
												boolean negative2 = spa2.isNegative();
												String sign2 = negative2 ? "-" : "+";
												String header2 = rule.getHouse().getName() + " " + 
													sign2 + " " + 
													(negative2 ? planet2.getNegative() : planet2.getPositive());
												section.add(new Paragraph(header2, fonth6));
												if (term) {
													if (acode.equals("CONJUNCTION"))
									    				section.add(new Paragraph((rus ? "Соединение дирекционных планет " : "") + planet.getName()
								    						+ (rus ? " и " : " and ") + planet2.getName() +
								    					(house.isAngled() ? (rus ? " на " : " on ") + house.getDesignation() :
								    						(rus ? " с куспидом сектора «" : " in sector «") + house.getName() + "» " + (rus ? "" : "of ") +
								    							house.getDesignation() + (rus ? " дома" : " house")), grayfont));
												}
												section.add(new Paragraph(PDFUtil.removeTags(rule.getText(), font)));
											}
										}
									}
								}
								//используется для связки двух домов (дирекционного и натального)
								if (!acode.equals("CONJUNCTION") && !acode.equals("OPPOSITION")) {
									House h = planet.getHouse();
									String comment = negative
										? "Сопутствовать этому будут следующие негативные факторы"
										: "Это станет возможным благодаря следующим позитивным факторам";
									String htext = negative ? h.getNegative() : h.getPositive();
									section.add(Chunk.NEWLINE);
									section.add(new Paragraph(comment + ": " + htext, grayfont));
								}
								PDFUtil.printGender(section, dirText, female, child, true, lang);
							}
							section.add(Chunk.NEWLINE);
						}
						Rule rule = EventRules.ruleHouseDirection(spa, female);
						if (rule != null)
							section.add(new Paragraph(PDFUtil.removeTags(rule.getText(), font)));
					}
				} else if (skyPoint instanceof Planet) {
					List<Model> texts = servicea.finds(spa);
					boolean negative = false;
					if (!texts.isEmpty()) {
						DirectionAspectText dirText = (DirectionAspectText)texts.get(0);
						negative = (null == dirText) ? type.getPoints() < 0 : !dirText.isPositive();
					}
					Planet planet2 = (Planet)skyPoint;
    				String pname = negative ? event.getPlanets().get(planet.getId()).getBadName() : event.getPlanets().get(planet.getId()).getGoodName();
    				String pname2 = negative ? event.getPlanets().get(planet2.getId()).getBadName() : event.getPlanets().get(planet2.getId()).getGoodName();
    				House house = planet.getHouse();
    				House house2 = planet2.getHouse();
    				String phouse = house.getName();
    				String phouse2 = house2.getName();

					String text = pname + "-" + phouse + " " + type.getSymbol() + " " + pname2 + "-" + phouse2;
    				section.addSection(new Paragraph(text, fonth5));

					if (!texts.isEmpty())
						for (Model model : texts) {
							DirectionAspectText dirText = (DirectionAspectText)model;
							if (dirText != null) {
								text = dirText.getText();
								if ((null == text || text.isEmpty())
										&& dirText.getAspect() != null)
									continue;
							}

							Aspect dasp = dirText.getAspect();
			    			if (term && null == dasp) {
								String pretext = acode.equals("CONJUNCTION")
									? "с натальной планетой"
									: "к натальной планете";

			    				p = new Paragraph();
					    		p.add(new Chunk(spa.getAspect().getName() + " дирекционной планеты ", grayfont));
					    		p.add(new Chunk(planet.getSymbol(), afont));
			    				p.add(new Chunk(" " + planet.getName(), grayfont));
								p.add(new Chunk(" из " + CalcUtil.roundTo(planet.getLongitude(), 2) + "° (", grayfont));
								Sign sign = planet.getSign();
			    				p.add(new Chunk(sign.getSymbol(), afont));
			    				p.add(new Chunk(" " + sign.getName(), grayfont));
			    				String mark = planet.getMark("sign", term, lang);
			    				p.add(new Chunk((mark.isEmpty() ? "" : " " + mark) + ", ", grayfont));
			    				p.add(new Chunk(house.getDesignation() + " дом, сектор «" + house.getName() + "»", grayfont));
			    				mark = planet.getMark("house", term, lang);
			    				p.add(new Chunk((mark.isEmpty() ? "" : " " + mark) + ") ", grayfont));
					    		p.add(new Chunk(pretext + " ", grayfont));
			    				p.add(new Chunk(planet2.getSymbol(), afont));
			    				p.add(new Chunk(" " + planet2.getName(), grayfont));
			    				if (!acode.equals("CONJUNCTION")) {
									Sign sign2 = planet2.getSign();
				    				p.add(new Chunk(" (" + sign2.getSymbol(), afont));
				    				p.add(new Chunk(" " + sign2.getName() + ", ", grayfont));
				    				p.add(new Chunk(house2.getDesignation() + " дом, сектор «" + house2.getName() + "») ", grayfont));
			    				}
			    				section.add(p);
			    			}

			    			if (null == dasp) {
								if (acode.equals("QUADRATURE"))
									section.add(new Paragraph("Уровень критичности: высокий", red));
								else if (acode.equals("OPPOSITION"))
									section.add(new Paragraph("Уровень критичности: средний", orange));
								else if (acode.equals("TRIN"))
									section.add(new Paragraph("Уровень успеха: высокий", red));
								else if (acode.equals("SEXTILE"))
									section.add(new Paragraph("Уровень успеха: средний", orange));
			    			}
	
							if (dirText != null) {
								text = dirText.getText();
								if (text != null) {
					    			String typeColor = type.getFontColor();
									BaseColor color = PDFUtil.htmlColor2Base(typeColor);
									section.add(new Paragraph(PDFUtil.removeTags(text, new Font(baseFont, 12, Font.NORMAL, color))));
								}
								//используется для связки двух домов (дирекционного и натального)
								if (null == dasp) {
									House h = planet.getHouse();
									String comment = "";
									if (acode.equals("CONJUNCTION"))
										comment = "Всё это актуально для следующих сфер жизни: " + h.getDescription();
									else {
										House h2 = planet2.getHouse();
										comment = negative
											? "Уязвимой здесь является сфера жизни «" + h2.getName() + "». На неё повлияют следующие негативные факторы"
											: "Данный позитивный прогноз касается сферы жизни «" + h2.getName() + "», и этому поспособствуют следующие благоприятные факторы";
										comment += ": " + (negative ? h.getNegative() : h.getPositive());
									}
									section.add(Chunk.NEWLINE);
									section.add(new Paragraph(comment, grayfont));
									PDFUtil.printGender(section, dirText, female, child, true, lang);

									//правило домов
									DirectionRule rule = servicer.findRule(planet, house, spa.getAspect().getType(), planet2, house2);
									if (rule != null) {
										Aspect aspect = rule.getAspect();
										if (aspect != null
												&& !aspect.getId().equals(spa.getAspect().getId()))
											continue;
		
										section.add(Chunk.NEWLINE);
										section.add(new Paragraph(PDFUtil.removeTags(rule.getText(), font)));
										PDFUtil.printGender(section, rule, female, child, true, lang);
									}
								}
							}
							section.add(Chunk.NEWLINE);
						}
				}
			}
			return section;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Поиск поражённых и непоражённых планет
	 * @throws DataAccessException 
	 */
	@SuppressWarnings("unused")
	private void initPlanetStatistics(Event event, List<SkyPointAspect> spas) throws DataAccessException {
		PlanetService service = new PlanetService();
		List<Model> positions = new PositionTypeService().getList();
		for (SkyPointAspect spa : spas) {
			Planet planet = (Planet)spa.getSkyPoint1();
			if (!planet.isDone()) {
				//афетика
				for (Model type : positions) {
					PositionType pType = (PositionType)type; 
					String pCode = pType.getCode();
//					Sign sign = service.getSignPosition(planet, pCode, true);
//					if (sign != null && sign.getId() == planet.getSign().getId()) {
//						switch (pCode) {
//							case "HOME": planet.setSignHome(); break;
//							case "EXALTATION": planet.setSignExaltated(); break;
//							case "EXILE": planet.setSignExile(); break;
//							case "DECLINE": planet.setSignDeclined(); break;
//						}
//					}

					if (null == planet.getHouse()) continue;
//					Map<Long, House> houses = service.getHousePosition(planet, pCode, true);
//					if (houses != null && houses.containsKey(planet.getHouse().getId())) {
//						switch (pCode) {
//							case "HOME": planet.setHouseHome(); break;
//							case "EXALTATION": planet.setHouseExaltated(); break;
//							case "EXILE": planet.setHouseExile(); break;
//							case "DECLINE": planet.setHouseDeclined(); break;
//						}
//					}
				}

				//аспекты, влияющие на статус планеты
				if (spa.getSkyPoint2() instanceof Planet) {
					if (spa.getAspect().getCode().equals("CONJUNCTION")) {
						String code2 = spa.getSkyPoint2().getCode();
						if (code2.equals("Lilith"))
							planet.setLilithed();
						else if (code2.equals("Kethu"))
							planet.setKethued();
					}
				}
				planet.setDone(true);
			}
		}
	}

	/**
	 * Выводим описание диаграммы возраста
	 * @param section раздел
	 */
	private void printDiagramDescr(Section section) {
		if (OsUtil.getOS().equals(OsUtil.OS.LINUX)) {
			String text = "Диаграмма обобщает информацию о событиях возраста и указывает, какие сферы жизни будут актуальны в течение года:";
			section.add(new Paragraph(text, font));
	
			com.itextpdf.text.List list = new com.itextpdf.text.List(false, false, 10);
			ListItem li = new ListItem();
	        li.add(new Chunk("Показатели выше нуля указывают на успех и лёгкость", new Font(baseFont, 12, Font.NORMAL, new BaseColor(0, 102, 102))));
	        list.add(li);
	
			li = new ListItem();
	        li.add(new Chunk("Показатели на нуле указывают на нейтральность ситуации", font));
	        list.add(li);
	
			li = new ListItem();
	        li.add(new Chunk("Показатели ниже нуля указывают на трудности и напряжение", new Font(baseFont, 12, Font.NORMAL, new BaseColor(102, 0, 51))));
	        list.add(li);
	        section.add(list);
		} else {
			String text = "Диаграмма обобщает информацию о событиях возраста и указывает, какие сферы жизни будут актуальны в течение года. Чем длиньше столбик, тем больше успеха ожидается в данной сфере жизни";
			section.add(new Paragraph(text, font));			
		}
	}
}
