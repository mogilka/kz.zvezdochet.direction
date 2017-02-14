package kz.zvezdochet.direction.exporter;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Section;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfWriter;

import kz.zvezdochet.analytics.bean.PlanetAspectText;
import kz.zvezdochet.bean.AspectType;
import kz.zvezdochet.bean.Event;
import kz.zvezdochet.bean.House;
import kz.zvezdochet.bean.Place;
import kz.zvezdochet.bean.Planet;
import kz.zvezdochet.bean.SkyPoint;
import kz.zvezdochet.bean.SkyPointAspect;
import kz.zvezdochet.core.bean.TextGender;
import kz.zvezdochet.core.util.CoreUtil;
import kz.zvezdochet.core.util.DateUtil;
import kz.zvezdochet.core.util.PlatformUtil;
import kz.zvezdochet.core.util.StringUtil;
import kz.zvezdochet.direction.Activator;
import kz.zvezdochet.direction.bean.DirectionText;
import kz.zvezdochet.direction.service.DirectionAspectService;
import kz.zvezdochet.direction.service.DirectionService;
import kz.zvezdochet.export.handler.PageEventHandler;
import kz.zvezdochet.export.util.PDFUtil;

/**
 * Генератор PDF-файла для экспорта событий
 * @author Nataly Didenko
 *
 */
public class PDFExporter {
	private BaseFont baseFont;
	private Font font, fonth5;

	public PDFExporter() {
		try {
			baseFont = PDFUtil.getBaseFont();
			font = PDFUtil.getRegularFont(baseFont);
			fonth5 = PDFUtil.getHeaderFont(baseFont);
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
			Chapter chapter = new ChapterAutoNumber("Общая информация");
			chapter.setNumberDepth(0);

			//шапка
			Paragraph p = new Paragraph();
			PDFUtil.printHeader(p, "Прогноз событий", baseFont);
			chapter.add(p);

			String text = DateUtil.fulldtf.format(event.getBirth());
			p = new Paragraph(text, font);
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

			chapter.add(new Paragraph("Прогноз содержит как позитивные, так и негативные события. "
				+ "Негатив - признак того, что вам необходим отдых и переосмысление. "
				+ "Не зацикливайтесь на негативе, развивайте свои сильные стороны, используя благоприятные события.", font));
			chapter.add(new Paragraph("Если из возраста в возраст событие повторяется, значит оно создаст большой резонанс.", font));
			chapter.add(new Paragraph("Максимальная погрешность прогноза события ±1 год.", font));

			//данные для графика
			Map<Integer,Integer[]> positive = new HashMap<Integer,Integer[]>();
			Map<Integer,Integer[]> negative = new HashMap<Integer,Integer[]>();

			int ages = finalage - initage + 1;
			for (int i = 0; i < ages; i++) {
				int nextage = initage + i;

				Integer[] iarr = new Integer[3];
				for (int j = 0; j < 3; j++)
					iarr[j] = 0;
				positive.put(nextage, iarr);

				iarr = new Integer[3];
				for (int j = 0; j < 3; j++)
					iarr[j] = 0;
				negative.put(nextage, iarr);
			}

			//события
			Map<Integer, Map<String, List<SkyPointAspect>>> map = new HashMap<Integer, Map<String, List<SkyPointAspect>>>();
			for (SkyPointAspect spa : spas) {
				int age = (int)spa.getAge();
				Map<String, List<SkyPointAspect>> agemap = map.get(age);
				if (null == agemap) {
					agemap = new HashMap<String, List<SkyPointAspect>>();
					agemap.put("main", new ArrayList<SkyPointAspect>());
					agemap.put("strong", new ArrayList<SkyPointAspect>());
					agemap.put("inner", new ArrayList<SkyPointAspect>());
				}
				String code = spa.getAspect().getType().getCode();
				if (code.equals("NEUTRAL") || code.equals("NEGATIVE") || code.equals("POSITIVE")) {
					if (spa.getSkyPoint2() instanceof Planet) {
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
					if (code.equals("NEUTRAL")) {
						Planet planet = (Planet)spa.getSkyPoint1();
						String pcode = planet.getCode();
						if (pcode.equals("Lilith") || pcode.equals("Kethu")) {
							Integer[] arr = negative.get(age);
							arr[2] = arr[2] + 1;
							negative.put(age, arr);
						} else {
							Integer[] arr = positive.get(age);
							arr[0] = arr[0] + 1;
							positive.put(age, arr);
						}
					} else if (code.equals("POSITIVE")) {
						Integer[] arr = positive.get(age);
						arr[1] = arr[1] + 1;
						positive.put(age, arr);
					} else if (code.equals("NEGATIVE")) {
						Integer[] arr = negative.get(age);
						arr[2] = arr[2] + 1;
						negative.put(age, arr);
					}
				}
				map.put(age, agemap);
			}

			XYSeries seriesPositive = new XYSeries("Позитив");
			XYSeries seriesNegative = new XYSeries("Негатив");

			for (int i = 0; i < ages; i++)
				for (int j = 0; j < 3; j++) {
					int nextage = initage + i;
					seriesPositive.add(nextage, positive.get(nextage)[j]);
					seriesNegative.add(nextage, negative.get(nextage)[j]);
				}
			XYSeriesCollection dataset = new XYSeriesCollection();
			dataset.addSeries(seriesPositive);
			dataset.addSeries(seriesNegative);
			Image image = PDFUtil.printGraphics(writer, "Соотношение категорий событий", "Возраст", "Количество", dataset, 500, 0, true);
			chapter.add(image);
			doc.add(chapter);

			Map<Integer, Map<String, List<SkyPointAspect>>> treeMap = new TreeMap<Integer, Map<String, List<SkyPointAspect>>>(map);
			for (Map.Entry<Integer, Map<String, List<SkyPointAspect>>> entry : treeMap.entrySet()) {
			    int age = entry.getKey();
			    String agestr = CoreUtil.getAgeString(age);
				chapter = new ChapterAutoNumber(agestr);
				chapter.setNumberDepth(0);

				p = new Paragraph();
				PDFUtil.printHeader(p, agestr, baseFont);
				chapter.add(p);

			    Map<String, List<SkyPointAspect>> agemap = entry.getValue();
				for (Map.Entry<String, List<SkyPointAspect>> subentry : agemap.entrySet())
					printEvents(event, chapter, age, subentry.getKey(), subentry.getValue());
				doc.add(chapter);
			}
			doc.add(Chunk.NEWLINE);
	        doc.add(PDFUtil.printCopyright(baseFont));
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
			if (code.equals("main"))
				header += "Значимые события";
			else if (code.equals("strong"))
				header += "Менее значимые события";
			else if (code.equals("inner"))
				header += "Проявления личности";
			Section section = PDFUtil.printSection(chapter, header, baseFont);

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
					section.add(new Paragraph(planet.getShortName() + " " + type.getSymbol() + " " + house.getShortName(), fonth5));

					DirectionText dirText = (DirectionText)service.find(planet, house, type);
					if (dirText != null) {
						String typeColor = type.getFontColor();
						BaseColor color = PDFUtil.htmlColor2Base(typeColor);
						section.add(new Paragraph(StringUtil.removeTags(dirText.getText()), new Font(baseFont, 12, Font.NORMAL, color)));

						List<TextGender> genders = dirText.getGenderTexts(event.isFemale(), child);
						for (TextGender gender : genders) {
							Paragraph p = new Paragraph(PDFUtil.getGenderHeader(gender.getType()), fonth5);
							p.setSpacingBefore(10);
							section.add(p);
							section.add(new Paragraph(StringUtil.removeTags(gender.getText()), new Font(baseFont, 12, Font.NORMAL, color)));
						}
					}
				} else if (skyPoint instanceof Planet) {
					Planet planet2 = (Planet)skyPoint;
					if (planet.getNumber() > planet2.getNumber())
						continue;
					section.add(new Paragraph(planet.getShortName() + " " + type.getSymbol() + " " + planet2.getShortName(), fonth5));

					PlanetAspectText dirText = (PlanetAspectText)servicea.find(planet, planet2, spa.getAspect());
					if (dirText != null) {
						String typeColor = type.getFontColor();
						BaseColor color = PDFUtil.htmlColor2Base(typeColor);
						section.add(new Paragraph(StringUtil.removeTags(dirText.getText()), new Font(baseFont, 12, Font.NORMAL, color)));

						List<TextGender> genders = dirText.getGenderTexts(event.isFemale(), child);
						for (TextGender gender : genders) {
							Paragraph p = new Paragraph(PDFUtil.getGenderHeader(gender.getType()), fonth5);
							p.setSpacingBefore(10);
							section.add(p);
							section.add(new Paragraph(StringUtil.removeTags(gender.getText()), new Font(baseFont, 12, Font.NORMAL, color)));
						};
					}
				}
			}
			return section;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
