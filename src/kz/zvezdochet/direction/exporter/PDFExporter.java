package kz.zvezdochet.direction.exporter;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

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
import kz.zvezdochet.bean.Place;
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
import kz.zvezdochet.core.util.PlatformUtil;
import kz.zvezdochet.core.util.StringUtil;
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
 * @author Nataly Didenko
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
	public void generate(Event event, List<SkyPointAspect> spas, int initage, int finalage) {
		Document doc = new Document();
		try {
			String filename = PlatformUtil.getPath(Activator.PLUGIN_ID, "/out/events.pdf").getPath();
			PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(filename));
	        writer.setPageEvent(new PageEventHandler(doc));
	        doc.open();

	        //metadata
	        PDFUtil.getMetaData(doc, "Прогноз событий");

	        //раздел
			Chapter chapter = new ChapterAutoNumber(PDFUtil.printHeader(new Paragraph(), "Прогноз событий"));
			chapter.setNumberDepth(0);

			//шапка
			String text = DateUtil.fulldtf.format(event.getBirth());
			Paragraph p = new Paragraph(text, font);
	        p.setAlignment(Element.ALIGN_CENTER);
			chapter.add(p);

			Place place = event.getPlace();
			if (null == place)
				place = new Place().getDefault();
			text = (event.getZone() >= 0 ? "UTC+" : "") + event.getZone() +
					" " + (event.getDst() >= 0 ? "DST+" : "") + event.getDst() + 
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

	        p = new Paragraph("Прогноз содержит как позитивные, так и негативные события. "
				+ "Негатив - признак того, что вам необходим отдых, переосмысление и мобилизация ресурсов для решения проблемы. "
				+ "А также это возможность смягчить напряжение, ведь вы будете знать о нём заранее. "
				+ "Не зацикливайтесь на негативе, используйте свои сильные стороны и благоприятные события.", font);
	        p.setSpacingAfter(10);
			chapter.add(p);

			chapter.add(new Paragraph("Если из возраста в возраст событие повторяется, значит оно создаст большой резонанс.", font));
			chapter.add(new Paragraph("Максимальная погрешность прогноза события ±1 год.", font));

			p = new Paragraph("Если в прогнозе упомянуты люди, которых уже нет в живых (родители, супруги, родственники), "
				+ "значит речь идёт о людях, их заменяющих или похожих на них по характеру.", font);
			p.setSpacingBefore(10);
			chapter.add(p);

			//данные для графика
			Map<Integer,Integer> positive = new HashMap<Integer,Integer>();
			Map<Integer,Integer> negative = new HashMap<Integer,Integer>();

			int ages = finalage - initage + 1;
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
			Map<Integer, Map<String, List<SkyPointAspect>>> map = new HashMap<Integer, Map<String, List<SkyPointAspect>>>();
			for (SkyPointAspect spa : spas) {
				Planet planet = (Planet)spa.getSkyPoint1();
				String pcode = planet.getCode();
				if (pcode.equals("Kethu") && spa.getAspect().getCode().equals("OPPOSITION"))
					continue;

				int age = (int)spa.getAge();
				Map<String, List<SkyPointAspect>> agemap = map.get(age);
				if (null == agemap) {
					agemap = new HashMap<String, List<SkyPointAspect>>();
					agemap.put("main", new ArrayList<SkyPointAspect>());
					agemap.put("strong", new ArrayList<SkyPointAspect>());
					agemap.put("inner", new ArrayList<SkyPointAspect>());
				}
				boolean isHouse = spa.getSkyPoint2() instanceof House;

				String code = spa.getAspect().getType().getCode();
				if (code.equals("NEUTRAL") || code.equals("NEGATIVE") || code.equals("POSITIVE")) {
					if (!isHouse) {
						List<SkyPointAspect> list = agemap.get("inner");
						list.add(spa);
					} else {
						if (code.equals("NEUTRAL")) {
							List<SkyPointAspect> list = agemap.get("main");
							list.add(spa);
						} else {
							List<SkyPointAspect> list = agemap.get("strong");
							list.add(spa);
						}
					}
					double point = 0;
					if (code.equals("NEUTRAL")) {
						if (pcode.equals("Lilith") || pcode.equals("Kethu")) {
							negative.put(age, negative.get(age) + 1);
							point -= 2;
						} else {
							positive.put(age, positive.get(age) + 1);
							point += 2;
						}
					} else if (code.equals("POSITIVE")) {
						positive.put(age, positive.get(age) + 1);
						++point;
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
				bars[i] = new Bar(strage, positive.get(nextage), null, "Позитивные события");
				bars[i + ages] = new Bar(strage, negative.get(nextage) * (-1), null, "Негативные события");
			}
			Image image = PDFUtil.printStackChart(writer, "Соотношение категорий событий", "Возраст", "Количество", bars, 500, 400, true);
			chapter.add(image);
			chapter.add(Chunk.NEXTPAGE);

			//примечание
			chapter.add(new Paragraph("Примечание:", font));
			com.itextpdf.text.List list = new com.itextpdf.text.List(false, false, 10);
			ListItem li = new ListItem();
	        li.add(new Chunk("Зелёным цветом выделены позитивные события", new Font(baseFont, 12, Font.NORMAL, new BaseColor(0, 102, 102))));
	        list.add(li);

			li = new ListItem();
	        li.add(new Chunk("Красным цветом выделены негативные события", new Font(baseFont, 12, Font.NORMAL, new BaseColor(102, 0, 51))));
	        list.add(li);

			li = new ListItem();
	        li.add(new Chunk("Чёрным цветом выделены важные события", font));
	        list.add(li);
	        chapter.add(list);

	        chapter.add(Chunk.NEWLINE);
	        chapter.add(new Paragraph("Заголовки абзацев (например, «Добро + Прибыль») используются для структурирования текста и "
	        	+ "указывают на сферу жизни, к которой относится описываемое событие", font));
	        doc.add(chapter);

			HouseService serviceh = new HouseService();
			Map<Integer, Map<String, List<SkyPointAspect>>> treeMap = new TreeMap<Integer, Map<String, List<SkyPointAspect>>>(map);
			for (Map.Entry<Integer, Map<String, List<SkyPointAspect>>> entry : treeMap.entrySet()) {
			    int age = entry.getKey();
			    String agestr = CoreUtil.getAgeString(age);
				chapter = new ChapterAutoNumber(PDFUtil.printHeader(new Paragraph(), agestr));
				chapter.setNumberDepth(0);

			    Map<String, List<SkyPointAspect>> agemap = entry.getValue();
				for (Map.Entry<String, List<SkyPointAspect>> subentry : agemap.entrySet())
					printEvents(event, chapter, age, subentry.getKey(), subentry.getValue());

				//диаграмма возраста
				Section section = PDFUtil.printSection(chapter, "Диаграмма");
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
				image = PDFUtil.printBars(writer, agestr, "Сферы жизни", "Баллы", items, 500, 300, false, false);
				section.add(image);
				doc.add(chapter);
				doc.add(Chunk.NEXTPAGE);
			}
			
			if (term) {
				chapter = new ChapterAutoNumber(PDFUtil.printHeader(new Paragraph(), "Сокращения"));
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

			chapter = new ChapterAutoNumber(PDFUtil.printHeader(new Paragraph(), "Сокращения"));
			chapter.setNumberDepth(0);
			p = new Paragraph();
			PDFUtil.printHeader(p, "Диаграммы");
			chapter.add(p);

			text = "Диаграммы обобщают приведённую выше информацию и наглядно отображают сферы жизни, которые будут занимать вас в каждом конкретном возрасте.";
			chapter.add(new Paragraph(text, font));
	        chapter.add(Chunk.NEWLINE);

	        HouseMap[] houseMap = HouseMap.getMap();
	        for (HouseMap hmap : houseMap) {
	        	Section section = PDFUtil.printSection(chapter, hmap.name);
				XYSeriesCollection items = new XYSeriesCollection();
				List<String> descrs = new ArrayList<String>();
	        	for (int i = 0; i < 3; i++) {
	        		long houseid = hmap.houseids[i];
	        		Map<Integer, Double> hdata = seriesh.get(houseid);
					House house = (House)serviceh.find(houseid);
			        XYSeries series = new XYSeries(house.getName());
	        		if (null == hdata || 0 == hdata.size()) {
//	        			if (i > 0) {
//							series.add(initage, 0);
//					        items.addSeries(series);
//	        			}
	        			continue;
	        		}
					descrs.add(house.getName() + ": " + house.getDescription());
	        		SortedSet<Integer> keys = new TreeSet<Integer>(hdata.keySet());
	        		for (Integer key : keys)
						series.add(key, hdata.get(key));
			        items.addSeries(series);
	        	}	        	
				image = PDFUtil.printGraphics(writer, "", "Возраст", "Баллы", items, 500, 300, true);
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
	private Section printEvents(Event event, Chapter chapter, int age, String code, List<SkyPointAspect> spas) {
		try {
			String header = "";
			Paragraph p = null;
			String agestr = CoreUtil.getAgeString(age);
			if (code.equals("main")) {
				header += "Значимые события";
				p = new Paragraph("В данном разделе описаны жизненноважные, долгожданные, переломные события, "
					+ "которые произойдут в возрасте " + agestr + ", надолго запомнятся и повлекут за собой перемены", font);
			} else if (code.equals("strong")) {
				header += "Менее значимые события";
				p = new Paragraph("В данном разделе описаны краткосрочные события, "
					+ "которые произойдут в возрасте " + agestr + " и не будут иметь больших последствий", font);
			} else if (code.equals("inner")) {
				header += "Проявления личности";
				p = new Paragraph("В данном разделе описаны черты вашей личности, которые станут особенно яркими в возрасте " + agestr, font);
			}
			Section section = PDFUtil.printSection(chapter, header);
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

				Planet planet = (Planet)spa.getSkyPoint1();
				if (planet.isLilithed() && type.getCode().equals("NEUTRAL"))
					continue;

				SkyPoint skyPoint = spa.getSkyPoint2();
				if (skyPoint instanceof House) {
					House house = (House)skyPoint;
					if (term) {
						p = new Paragraph();
	    				p.add(new Chunk(planet.getMark("house"), fonth5));
	    				p.add(new Chunk(planet.getSymbol(), PDFUtil.getHeaderAstroFont()));
	    				p.add(new Chunk(" " + planet.getName() + " (", fonth5));

	    				if (planet.getSign().getCode().equals("Ophiuchus"))
	    					p.add(new Chunk("\u221E", fonth5));
	    				else
	    					p.add(new Chunk(planet.getSign().getSymbol(), PDFUtil.getHeaderAstroFont()));

	    				p.add(new Chunk(", " + planet.getHouse().getDesignation() + ") ", fonth5));
	    				
	    				if (spa.getAspect().getCode().equals("CONJUNCTION") || spa.getAspect().getCode().equals("OPPOSITION"))
	    					p.add(new Chunk(spa.getAspect().getSymbol(), PDFUtil.getHeaderAstroFont()));
	    				else
	    					p.add(new Chunk(type.getSymbol(), fonth5));

	    				p.add(new Chunk(" " + house.getDesignation() + " дом", fonth5));
	    				section.addSection(p);
	    				section.add(Chunk.NEWLINE);
					} else
						section.addSection(new Paragraph(planet.getShortName() + " " + type.getSymbol() + " " + house.getName(), fonth5));

					DirectionText dirText = (DirectionText)service.find(planet, house, type);
					if (dirText != null) {
						String typeColor = type.getFontColor();
						BaseColor color = PDFUtil.htmlColor2Base(typeColor);
						section.add(new Paragraph(StringUtil.removeTags(dirText.getText()), new Font(baseFont, 12, Font.NORMAL, color)));
						PDFUtil.printGender(section, dirText, female, child, true);
					}
					Rule rule = EventRules.ruleHouseDirection(spa, female);
					if (rule != null)
						section.add(new Paragraph(StringUtil.removeTags(rule.getText()), font));

				} else if (skyPoint instanceof Planet) {
					Planet planet2 = (Planet)skyPoint;
					if (planet.getNumber() > planet2.getNumber())
						continue;

					PlanetAspectText dirText = (PlanetAspectText)servicea.find(planet, planet2, spa.getAspect());
					if (null == dirText)
	    				section.add(new Paragraph(planet.getShortName() + " " + type.getSymbol() + " " + planet2.getShortName(), fonth5));
					else {
		    			if (term) {
		    				List<Model> planets = event.getConfiguration().getPlanets();
		    				int pindex = planets.indexOf(planet2);
		    				Planet aspl2 = (Planet)planets.get(pindex);

		    				section.add(new Chunk(dirText.getMark(planet, aspl2), fonth5));
		    				section.add(new Chunk(planet.getSymbol(), PDFUtil.getHeaderAstroFont()));
		    				section.add(new Chunk(" " + planet.getName() + " (", fonth5));

		    				if (planet.getSign().getCode().equals("Ophiuchus"))
		    					section.add(new Chunk("\u221E", fonth5));
		    				else
		    					section.add(new Chunk(planet.getSign().getSymbol(), PDFUtil.getHeaderAstroFont()));

		    				section.add(new Chunk(", " + planet.getHouse().getDesignation() + ") ", PDFUtil.getHeaderAstroFont()));
		    				
		    				if (spa.getAspect().getCode().equals("CONJUNCTION") || spa.getAspect().getCode().equals("OPPOSITION"))
		    					section.add(new Chunk(spa.getAspect().getSymbol(), PDFUtil.getHeaderAstroFont()));
		    				else
		    					section.add(new Chunk(type.getSymbol(), fonth5));

		    				section.add(new Chunk(" " + planet2.getSymbol(), PDFUtil.getHeaderAstroFont()));
		    				section.add(new Chunk(" " + planet2.getName() + " (", fonth5));
		    				if (planet2.getSign().getCode().equals("Ophiuchus"))
		    					section.add(new Chunk("\u221E", fonth5));
		    				else
		    					section.add(new Chunk(planet2.getSign().getSymbol(), PDFUtil.getHeaderAstroFont()));
		    				section.add(new Chunk(", " + planet2.getHouse().getDesignation() + ")", PDFUtil.getHeaderAstroFont()));
		    				section.add(Chunk.NEWLINE);
		    			} else
		    				section.add(new Paragraph(planet.getShortName() + " " + type.getSymbol() + " " + planet2.getShortName(), fonth5));

		    			String typeColor = type.getFontColor();
						BaseColor color = PDFUtil.htmlColor2Base(typeColor);
						section.add(new Paragraph(StringUtil.removeTags(dirText.getText()), new Font(baseFont, 12, Font.NORMAL, color)));
						PDFUtil.printGender(section, dirText, female, child, true);
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
		String text = "Диаграмма обобщает информацию о событиях возраста:";
		section.add(new Paragraph(text, font));

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
        section.add(list);
	}
}
