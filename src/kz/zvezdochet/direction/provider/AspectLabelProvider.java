package kz.zvezdochet.direction.provider;

import org.eclipse.swt.graphics.Color;

import kz.zvezdochet.bean.SkyPointAspect;
import kz.zvezdochet.core.ui.ArrayLabelProvider;

/**
 * Формат таблицы аспектов
 * @author Natalie Didenko
 */
public class AspectLabelProvider extends ArrayLabelProvider {
	@Override
	public String getColumnText(Object element, int columnIndex) {
		SkyPointAspect aspect = (SkyPointAspect)element;
		switch (columnIndex) {
		case 0: return aspect.getSkyPoint1().getName();
		case 1: String text = "";
			if (aspect.isExact())
				text += "•";
			if (aspect.isApplication())
				text += "⇥";
			text += aspect.getScore();
			return text;
		case 2: return aspect.getSkyPoint2().getName();
		}
		return null;
	}
	@Override
	public Color getBackground(Object element, int columnIndex) {
		SkyPointAspect aspect = (SkyPointAspect)element;
		if (aspect.getAspect() != null)
			return aspect.getAspect().getType().getDimColor();
		return super.getForeground(element, columnIndex);
	}
}
