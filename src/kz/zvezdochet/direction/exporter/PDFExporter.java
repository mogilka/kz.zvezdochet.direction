package kz.zvezdochet.direction.exporter;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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

import kz.zvezdochet.analytics.bean.PlanetAspectText;
import kz.zvezdochet.analytics.bean.Rule;
import kz.zvezdochet.analytics.exporter.EventRules;
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
import kz.zvezdochet.core.util.DateUtil;
import kz.zvezdochet.core.util.OsUtil;
import kz.zvezdochet.core.util.PlatformUtil;
import kz.zvezdochet.direction.Activator;
import kz.zvezdochet.direction.bean.DirectionText;
import kz.zvezdochet.direction.service.DirectionAspectService;
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

	public PDFExporter() {
		try {
			baseFont = PDFUtil.getBaseFont();
			font = PDFUtil.getRegularFont();
			fonth5 = PDFUtil.getHeaderFont();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Генерация событий периода
	 * @param event событие
	 */
	public void generate(Event event, List<SkyPointAspect> spas, int initage, int finalage, boolean optimistic) {
		this.optimistic = optimistic;
		Document doc = new Document();
		try {
			String filename = PlatformUtil.getPath(Activator.PLUGIN_ID, "/out/long.pdf").getPath();
			PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(filename));
	        writer.setPageEvent(new PageEventHandler());
	        doc.open();

	        //metadata
	        PDFUtil.getMetaData(doc, "Долгосрочный прогноз");

	        //раздел
			Chapter chapter = new ChapterAutoNumber(PDFUtil.printHeader(new Paragraph(), "Долгосрочный прогноз", null));
			chapter.setNumberDepth(0);

			//шапка
	        int ages = finalage - initage + 1;
			String text = event.getCallname();
			text += " - прогноз на " + CoreUtil.getAgeString(ages);
			Paragraph p = new Paragraph(text, font);
	        p.setAlignment(Element.ALIGN_CENTER);
			chapter.add(p);

			p = new Paragraph("Тип прогноза: " + (optimistic ? "оптимистичный" : "реалистичный"), font);
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

	        boolean chartable = ages > 4;
	        chapter.add(new Paragraph("Данный прогноз не содержит конкретных дат, "
	        	+ "но описывает самые значительные тенденции вашей жизни в ближайшие " + CoreUtil.getAgeString(ages)
        		+ " независимо от переездов и местоположения.", font));
			Font red = PDFUtil.getDangerFont();
			chapter.add(new Paragraph("Максимальная погрешность прогноза ±2 месяца.", red));

			p = new Paragraph("Если в прогнозе упомянуты люди, которых уже нет в живых (родители, супруги, родственники), "
				+ "значит речь идёт о людях, их заменяющих (опекуны, крёстные) или похожих на них по характеру.", font);
			p.setSpacingBefore(10);
			chapter.add(p);

			//данные для графика
			Map<Integer,Integer> positive = new HashMap<Integer,Integer>();
			Map<Integer,Integer> negative = new HashMap<Integer,Integer>();

			for (int i = 0; i < ages; i++) {
				int nextage = initage + i;
				positive.put(nextage, 0);
				negative.put(nextage, 0);
			}

			//инициализация статистики планет
//			initPlanetStatistics(event, spas);

			Map<Long, Map<Integer, Double>> seriesh = new HashMap<Long, Map<Integer, Double>>();
			Map<Integer, Map<Long, Double>> seriesa = new HashMap<Integer, Map<Long, Double>>();

			//события
			Map<Integer, TreeMap<Integer, List<SkyPointAspect>>> map = new HashMap<Integer, TreeMap<Integer, List<SkyPointAspect>>>();
			for (SkyPointAspect spa : spas) {
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

				int age = (int)spa.getAge();
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

			Bar[] bars = new Bar[ages * 2];
			for (int i = 0; i < ages; i++) {
				int nextage = initage + i;
				String strage = CoreUtil.getAgeString(nextage);
				bars[i] = new Bar(strage, positive.get(nextage), null, "Позитивные события", null);
				bars[i + ages] = new Bar(strage, negative.get(nextage) * (-1), null, "Негативные события", null);
			}
			int height = 400;
			if (ages < 2)
				height = 150;
			else if (ages < 4)
				height = 200;
			Image image = PDFUtil.printStackChart(writer, "Соотношение категорий событий", "Возраст", "Количество", bars, 500, height, true);
			chapter.add(image);
			if (ages > 2)
				chapter.add(Chunk.NEXTPAGE);
			else
				chapter.add(Chunk.NEWLINE);

			Font bold = new Font(baseFont, 12, Font.BOLD);
			chapter.add(new Paragraph("Примечание:", bold));
			com.itextpdf.text.List list = new com.itextpdf.text.List(false, false, 10);
	        ListItem li = new ListItem();
	        li.add(new Chunk("Чёрным цветом выделены важные тенденции, которые указывают на основополагающие события периода.", font));
	        list.add(li);

			Font green = PDFUtil.getSuccessFont();
			li = new ListItem();
	        li.add(new Chunk("Зелёным цветом выделены позитивные тенденции. "
	        	+ "К ним относятся к событиям, которые сами по себе удачно складываются " 
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

	        if (ages > 1) {
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
				section.add(PDFUtil.printBars(writer, "", "Сферы жизни", "Баллы", items, 500, 300, false, false, false));
				section.add(new Paragraph("Ниже приведён прогноз по этим сферам жизни", font));
				chapter.add(Chunk.NEXTPAGE);

				for (Map.Entry<Integer, List<SkyPointAspect>> subentry : agemap.entrySet())
					printEvents(event, chapter, age, subentry.getKey(), subentry.getValue());
				doc.add(chapter);
				doc.add(Chunk.NEXTPAGE);
			}
			
			if (term) {
				chapter = new ChapterAutoNumber(PDFUtil.printHeader(new Paragraph(), "Сокращения", null));
				chapter.setNumberDepth(0);

				chapter.add(new Paragraph("Раздел событий:", font));
				list = new com.itextpdf.text.List(false, false, 10);
				li = new ListItem();
		        li.add(new Chunk("\u2191 — сильная планета, адекватно проявляющая себя в астрологическом доме", font));
		        list.add(li);

				li = new ListItem();
		        li.add(new Chunk("\u2193 — ослабленная планета, источник неуверенности, стресса и препятствий", font));
		        list.add(li);

				li = new ListItem();
		        li.add(new Chunk("обт — указанный астрологический дом является обителью планеты и облегчает ей естественное и свободное проявление", font));
		        list.add(li);

				li = new ListItem();
		        li.add(new Chunk("экз — указанный астрологический дом является местом экзальтации планеты, усиливая её проявления и уравновешивая слабые качества", font));
		        list.add(li);

				li = new ListItem();
		        li.add(new Chunk("пдн — указанный астрологический дом является местом падения планеты, где она чувствует себя «не в своей тарелке»", font));
		        list.add(li);

				li = new ListItem();
		        li.add(new Chunk("изг — указанный астрологический дом является местом изгнания планеты, ослабляя её проявления и усиливает негатив", font));
		        list.add(li);
		        chapter.add(list);

		        chapter.add(Chunk.NEWLINE);
				chapter.add(new Paragraph("Раздел личности:", font));
				list = new com.itextpdf.text.List(false, false, 10);
				li = new ListItem();
		        li.add(new Chunk("\u2191 — усиленный аспект, проявляющийся ярче других аспектов указанных планет (хорошо для позитивных сочетаний, плохо для негативных)", font));
		        list.add(li);

				li = new ListItem();
		        li.add(new Chunk("\u2193 — ослабленный аспект, проявляющийся менее ярко по сравнению с другими аспектами указанных планет (плохо для позитивных сочетаний, хорошо для негативных)", font));
		        list.add(li);
		        chapter.add(list);
				doc.add(chapter);
			}

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
	        doc.add(PDFUtil.printCopyright());
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
	        doc.close();
		}
	}

	/**
	 * Генерация событий по категориям
	 */
	private Section printEvents(Event event, Chapter chapter, int age, int code, List<SkyPointAspect> spas) {
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
			boolean child = age < event.MAX_TEEN_AGE;

			for (SkyPointAspect spa : spas) {
				AspectType type = spa.getAspect().getType();
				if (type.getCode().contains("HIDDEN"))
					continue;

				if (optimistic && 2 == spa.getAspect().getTypeid())
					continue;

				String[] pnegative = {"Lilith", "Kethu"};
				Planet planet = (Planet)spa.getSkyPoint1();
				SkyPoint skyPoint = spa.getSkyPoint2();
				if (optimistic
						&& 1 == spa.getAspect().getTypeid()
						&& (Arrays.asList(pnegative).contains(planet.getCode())
							|| Arrays.asList(pnegative).contains(skyPoint.getCode())))
					continue;

				String acode = spa.getAspect().getCode();

				if (skyPoint instanceof House) {
					House house = (House)skyPoint;
					DirectionText dirText = (DirectionText)service.find(planet, house, type);

					boolean negative = type.getPoints() < 0;
    				String pname = negative ? planet.getNegative() : planet.getPositive();
					section.addSection(new Paragraph(house.getName() + " " + type.getSymbol() + " " + pname, fonth5));
					if (term) {
						String pretext = spa.getAspect().getCode().equals("CONJUNCTION")
							? "с куспидом"
							: "к куспиду";

						p = new Paragraph();
	    				p.add(new Chunk(planet.getMark("house"), grayfont));
			    		p.add(new Chunk(spa.getAspect().getName() + " планеты ", grayfont));
	    				p.add(new Chunk(planet.getSymbol(), PDFUtil.getHeaderAstroFont()));
			    		p.add(new Chunk(" " + planet.getName() + " " + pretext + " " + house.getDesignation(), grayfont));
	    				section.add(p);
					}

					if (dirText != null) {
						if (acode.equals("QUADRATURE"))
							section.add(new Paragraph("Уровень критичности: высокий", red));
						else if (acode.equals("OPPOSITION"))
							section.add(new Paragraph("Уровень критичности: средний", red));
						else if (acode.equals("TRIN"))
							section.add(new Paragraph("Уровень успеха: высокий", orange));
						else if (acode.equals("SEXTILE"))
							section.add(new Paragraph("Уровень успеха: средний", orange));

						String text = dirText.getText();
						if (text != null) {
							String typeColor = type.getFontColor();
							BaseColor color = PDFUtil.htmlColor2Base(typeColor);
							section.add(new Paragraph(PDFUtil.removeTags(text, new Font(baseFont, 12, Font.NORMAL, color))));
							PDFUtil.printGender(section, dirText, female, child, true);
						}
					}
					Rule rule = EventRules.ruleHouseDirection(spa, female);
					if (rule != null)
						section.add(new Paragraph(PDFUtil.removeTags(rule.getText(), font)));

				} else if (skyPoint instanceof Planet) {
					Planet planet2 = (Planet)skyPoint;
    				section.addSection(new Paragraph(planet.getShortName() + " " + type.getSymbol() + " " + planet2.getShortName(), fonth5));
					List<Model> texts = servicea.finds(spa);
					if (!texts.isEmpty())
						for (Model model : texts) {
							PlanetAspectText dirText = (PlanetAspectText)model;
							if (dirText != null) {
								String text = dirText.getText();
								if ((null == text || text.isEmpty())
										&& dirText.getAspect() != null)
									continue;
							}

			    			if (term) {
								String pretext = spa.getAspect().getCode().equals("CONJUNCTION")
									? "с планетой"
									: "к планете";

			    				p = new Paragraph();
			    				if (dirText != null)
			    					p.add(new Chunk(dirText.getMark(), grayfont));
					    		p.add(new Chunk(spa.getAspect().getName() + " планеты ", grayfont));
			    				p.add(new Chunk(planet.getSymbol(), PDFUtil.getHeaderAstroFont()));
					    		p.add(new Chunk(" " + planet.getName() + " " + pretext + " ", grayfont));
			    				p.add(new Chunk(planet2.getSymbol(), PDFUtil.getHeaderAstroFont()));
			    				p.add(new Chunk(" " + planet2.getName(), grayfont));
			    				section.add(p);
			    			}

			    			if (null == dirText.getAspect()) {
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
								String text = dirText.getText();
								if (text != null) {
					    			String typeColor = type.getFontColor();
									BaseColor color = PDFUtil.htmlColor2Base(typeColor);
									section.add(new Paragraph(PDFUtil.removeTags(text, new Font(baseFont, 12, Font.NORMAL, color))));
									PDFUtil.printGender(section, dirText, female, child, true);
								}
							}
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
					boolean daily = true;
					if (!planet.getCode().equals("Sun") &&
							(pCode.equals("HOME") || pCode.equals("EXILE")))
						daily = DateUtil.isDaily(event.getBirth());

					Sign sign = service.getSignPosition(planet, pCode, daily);
					if (sign != null && sign.getId() == planet.getSign().getId()) {
						switch (pCode) {
							case "HOME": planet.setSignHome(); break;
							case "EXALTATION": planet.setSignExaltated(); break;
							case "EXILE": planet.setSignExile(); break;
							case "DECLINE": planet.setSignDeclined(); break;
						}
					}

					if (null == planet.getHouse()) continue;
					House house = service.getHousePosition(planet, pCode, daily);
					int hnumber = CalcUtil.trunc((planet.getHouse().getNumber() + 2) / 3);
					if (house != null && CalcUtil.trunc((house.getNumber() + 2) / 3) == hnumber) {
						switch (pCode) {
							case "HOME": planet.setHouseHome(); break;
							case "EXALTATION": planet.setHouseExaltated(); break;
							case "EXILE": planet.setHouseExile(); break;
							case "DECLINE": planet.setHouseDeclined(); break;
						}
					}
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
