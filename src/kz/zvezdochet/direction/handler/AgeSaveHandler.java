package kz.zvezdochet.direction.handler;

import java.util.List;

import org.eclipse.e4.core.contexts.Active;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;

import kz.zvezdochet.bean.Event;
import kz.zvezdochet.bean.SkyPointAspect;
import kz.zvezdochet.core.handler.Handler;
import kz.zvezdochet.core.ui.util.DialogUtil;
import kz.zvezdochet.direction.exporter.PDFExporter;
import kz.zvezdochet.direction.part.AgePart;

/**
 * Сохранение дирекций периода в файл
 * @author Natalie Didenko
 *
 */
public class AgeSaveHandler extends Handler {

	@Execute
	public void execute(@Active MPart activePart) {
		try {
			final AgePart agePart = (AgePart)activePart.getObject();
			@SuppressWarnings("unchecked")
			final List<SkyPointAspect> spas = (List<SkyPointAspect>)agePart.getData();
			if (null == spas) return;
			final Event event = agePart.getEvent();
			final int choice = DialogUtil.alertQuestion("Вопрос", "Выберите тип прогноза:", new String[] {"Реалистичный", "Оптимистичный"});
			updateStatus("Сохранение дирекций в файл", false);

			final Display display = Display.getDefault();
    		BusyIndicator.showWhile(display, new Runnable() {
    			@Override
    			public void run() {
    				new PDFExporter().generate(event, spas, agePart.getInitialAge(), agePart.getFinalAge(), choice > 0);
    			}
    		});
			//TODO показывать диалог, что документ сформирован
			//а ещё лучше открывать его
			updateStatus("Файл событий сформирован", false);
		} catch (Exception e) {
			DialogUtil.alertConfirm(e.getMessage());
			updateStatus("Ошибка", true);
			e.printStackTrace();
		}
	}
}
