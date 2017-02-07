package kz.zvezdochet.direction.exporter;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chapter;
import com.itextpdf.text.ChapterAutoNumber;
import com.itextpdf.text.Document;
import com.itextpdf.text.Font;
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
import kz.zvezdochet.service.AspectTypeService;

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
		try {
			Document doc = new Document();
			String filename = PlatformUtil.getPath(Activator.PLUGIN_ID, "/out/events.pdf").getPath();
			PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(filename));
	        writer.setPageEvent(new PageEventHandler(doc));
	        doc.open();

	        //metadata
	        PDFUtil.getMetaData(doc, "Прогноз событий");

	        //раздел
			Chapter chapter = new ChapterAutoNumber("Общая информация");
			chapter.setNumberDepth(0);

			//дата события
			Paragraph p = new Paragraph();
			Place place = event.getPlace();
			if (null == place)
				place = new Place().getDefault();
			String text = DateUtil.fulldtf.format(event.getBirth()) +
				" " + (event.getZone() >= 0 ? "UTC+" : "") + event.getZone() +
				" " + (event.getDst() >= 0 ? "DST+" : "") + event.getDst() + 
				" " + place.getName() +
				" " + place.getLatitude() + "°" +
				", " + place.getLongitude() + "°";
			PDFUtil.printHeader(p, text, baseFont);
			chapter.add(p);

			chapter.add(new Paragraph("Прогноз содержит как позитивные, так и негативные события. "
				+ "Негатив - признак того, что вам необходим отдых и переосмысление. "
				+ "Не зацикливайтесь на негативе, развивайте свои сильные стороны, используя благоприятные события.", font));
			chapter.add(new Paragraph("Если из возраста в возраст события повторяются, значит они создают большой резонанс. "
				+ "Максимальная погрешность прогноза события ±1 год.", font));
			doc.add(chapter);

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
				}
				map.put(age, agemap);
			}
			for (Map.Entry<Integer, Map<String, List<SkyPointAspect>>> entry : map.entrySet()) {
			    int age = entry.getKey();
				chapter = new ChapterAutoNumber(CoreUtil.getAgeString(age));
				chapter.setNumberDepth(0);

			    Map<String, List<SkyPointAspect>> agemap = entry.getValue();
				for (Map.Entry<String, List<SkyPointAspect>> subentry : agemap.entrySet()) {
					Section section = printEvents(event, chapter, age, subentry.getKey(), subentry.getValue());
					chapter.add(section);
				}
			}
		} catch(Exception e) {
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Ошибка", e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Генерация событий по категориям
	 */
	private Section printEvents(Event event, Chapter chapter, int age, String code, List<SkyPointAspect> spas) {
		try {
			String header = CoreUtil.getAgeString(age) + ": ";
			if (code.equals("main"))
				header += "Главные события";
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
				String tcode = type.getCode();
				if (tcode.contains("HIDDEN")) {
					if (tcode.contains("NEGATIVE"))
						type = (AspectType)new AspectTypeService().find("NEGATIVE");
					else if (tcode.contains("POSITIVE"))
						type = (AspectType)new AspectTypeService().find("POSITIVE");
				}

				Planet planet = (Planet)spa.getSkyPoint1();
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

					PlanetAspectText dirText = (PlanetAspectText)servicea.find(planet, planet2, type);
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
