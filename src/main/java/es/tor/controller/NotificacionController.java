package es.tor.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import es.tor.push.AndroidPush;
import es.tor.dao.CanalDao;
import es.tor.dao.DispositivoCanalDao;
import es.tor.dao.DeviceDao;
import es.tor.dao.NotificationDao;
import es.tor.entity.Canal;
import es.tor.entity.Device;
import es.tor.entity.Notification;
import es.tor.push.ApplePush;

@Controller
@RequestMapping(value = "/notificacion")
public class NotificacionController extends GenericController {
    @Autowired
    protected NotificationDao notificacionDao;

    @Autowired
    protected DeviceDao dispositivoDao;

    @Autowired
    protected CanalDao canalDao;

    @Autowired
    protected DispositivoCanalDao dispositivoCanalDao;

    private static Notification ultima;

    @RequestMapping(value = "/listado.htm", method = RequestMethod.GET)
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ModelAndView list(@RequestParam(value = "start", required = false) Integer start) {
        if (start == null)
            start = 0;

        Map<String, Object> modelo = new HashMap<String, Object>();
        modelo.put("mensaje", "");

        Long count = notificacionDao.count();
        modelo.put("count", count);
        if ((start + NotificationDao.NUM_RESULTS_PAGINATOR) <= count) {
            modelo.put("next", start + NotificationDao.NUM_RESULTS_PAGINATOR);
        } else {
            modelo.put("next", -1);
        }
        if (start >= NotificationDao.NUM_RESULTS_PAGINATOR) {
            modelo.put("previous", start - NotificationDao.NUM_RESULTS_PAGINATOR);
        } else {
            modelo.put("previous", -1);
        }

        modelo.put("notificaciones", notificacionDao.findAll(start));

        return new ModelAndView("notificacion/listado", "modelo", modelo);
    }

    @RequestMapping(value = "/enviar.htm", method = RequestMethod.GET)
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ModelAndView send(HttpServletRequest request) {
        Notification notificacion;
        try {
            Long idNotificacion = Long.parseLong(request.getParameter("idNotificacion"));
            notificacion = notificacionDao.findById(idNotificacion);
        } catch (Exception e) {
            notificacion = null;
        }
        if (notificacion == null)
            notificacion = new Notification();

        Map<String, Object> modelo = new HashMap<String, Object>();
        modelo.put("mensaje", "");
        modelo.put("notificacion", notificacion);
        modelo.put("canales", canalDao.findAll(request.isUserInRole("ROLE_ADMIN")));

        return new ModelAndView("notificacion/enviar", modelo);
    }

    @RequestMapping(value = "/enviar.htm", method = RequestMethod.POST)
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public String sendAllWeb(HttpServletRequest request, HttpServletResponse response,
                    @ModelAttribute Notification notificacion) {

        notificacion.setCanal(canalDao.findById(notificacion.getCanal().getIdCanal()));

        Long idNotificacion;
        if (notificacion.getIdNotificacion() != null && notificacion.getIdNotificacion() > 0) {
            idNotificacion = update(request, notificacion.getIdNotificacion(), notificacion);

        } else {
            idNotificacion = send(request, notificacion);
        }

        if (idNotificacion != null && idNotificacion != 0) {
            return "redirect:/notificacion/listado.htm";
        } else {
            return "redirect:/notificacion/enviar.htm?idNotificacion=" + notificacion.getIdNotificacion();
        }
    }

    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    public List<Notification> getNotificaciones() {
        return notificacionDao.findAll(0);
    }

    @RequestMapping(value = "/{idDispositivo}", method = RequestMethod.GET)
    @ResponseBody
    public List<Notification> getNotificacionesById(@PathVariable("idDispositivo") Long idDispositivo) {
        Device dispositivo = dispositivoDao.findById(idDispositivo);
        if (dispositivo == null)
            return null;

        List<Canal> canales = dispositivoCanalDao.findCanalesDispositivo(dispositivo.getIdDispositivo());
        if (canales.size() == 0)
            return new ArrayList<Notification>();

        StringBuffer canalesStr = new StringBuffer();
        for (Canal canal : canales) {
            if (canalesStr.length() != 0)
                canalesStr.append(",");
            canalesStr.append(canal.getIdCanal());
        }

        List<Notification> notificaciones = notificacionDao.findByCanales(canalesStr.toString());

        // Si el idioma del dispositivo es valenciano, cambiamos el texto
        if (dispositivo.getIdioma() != null && dispositivo.getIdioma().equals(2)) {
            for (Notification notificacion : notificaciones) {
                if (notificacion.getMensajeVa() != null && !notificacion.getMensajeVa().isEmpty()) {
                    notificacion.setMensaje(notificacion.getMensajeVa());
                }
            }
        }

        return notificaciones;
    }

    @RequestMapping(value = "/{idNotificacion}", method = RequestMethod.POST)
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void update(HttpServletRequest request, HttpServletResponse response,
                    @PathVariable("idNotificacion") Long idNotificacion, @ModelAttribute Notification notificacion)
                    throws Exception {

        notificacion.setCanal(canalDao.findById(notificacion.getCanal().getIdCanal()));

        String idNotificacionStr = String.valueOf(update(request, idNotificacion, notificacion));
        response.setContentType("text/plain");
        response.getOutputStream().write(idNotificacionStr.getBytes());
        response.setContentLength(idNotificacionStr.length());
        response.setStatus(200);
    }

    public Long update(HttpServletRequest request, Long idNotificacion, Notification notificacion) {
        Notification notificacionOld = notificacionDao.findById(idNotificacion);
        notificacionOld.setFecha(new Date());
        notificacionOld.setMensaje(notificacion.getMensaje());
        notificacionOld.setMensajeVa(notificacion.getMensajeVa());
        notificacionOld.setTitulo(notificacion.getTitulo());
        notificacionOld.setCanal(notificacion.getCanal());

        return send(request, notificacionOld);
    }

    @RequestMapping(method = RequestMethod.POST)
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void sendNotificacion(HttpServletRequest request, HttpServletResponse response,
                    @ModelAttribute Notification notificacion) throws Exception {

        notificacion.setCanal(canalDao.findById(notificacion.getCanal().getIdCanal()));

        String idNotificacionStr = String.valueOf(send(request, notificacion));
        response.getOutputStream().write(idNotificacionStr.getBytes());
        response.setContentType("text/plain");
        response.setContentLength(idNotificacionStr.length());
        response.setStatus(200);
    }

    protected synchronized Long send(HttpServletRequest request, Notification notificacion) {
        List<Device> dispositivos;

        if (ultima != null && ultima.getIdCanal().equals(notificacion.getIdCanal())) {
            long diff = new Date().getTime() - ultima.getFecha().getTime();
            // Una diferencia de menos de x segundos entre una y otra significa
            // que nos han llegado dos peticiones iguales
            if (diff < (1000 * 15)) {
                return ultima.getIdNotificacion();
            }
        }

        if ((notificacion.getCanal().getIdCanal().equals(CanalDao.ID_CANAL_BROADCAST))
                        && request.isUserInRole("ROLE_ADMIN")) {
            dispositivos = dispositivoDao.findAll();
        } else {
            dispositivos = dispositivoDao.findByCanal(notificacion.getCanal().getIdCanal());
        }
        if (dispositivos == null || dispositivos.size() == 0) {
            notificacion = notificacionDao.save(notificacion);
            ultima = notificacion;

            return notificacion.getIdNotificacion();
        }

        AndroidPush androidPush = new AndroidPush(dispositivoDao);
        androidPush.sendAndroid(notificacion, dispositivos);
        try {
            ApplePush applePush = new ApplePush(dispositivoDao);
            applePush.sendApple(notificacion, dispositivos, true);
        } catch (Exception e) {
            logger.error("Error inesperado al mandar notificaciones iOS", e);
        }

        notificacion = notificacionDao.save(notificacion);
        ultima = notificacion;

        return notificacion.getIdNotificacion();
    }

    @RequestMapping(value = "/testApple", method = RequestMethod.GET)
    @ResponseBody
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public String testSendApple(HttpServletRequest request) {
        Device iphone = new Device();
        iphone.setIdDispositivo(1000L);
        iphone.setTipo(Device.APPLE);
        // "f531dd54130b04ee956be120d33938f46fa4b04e9ba82c455da7ba193ce667a8"
        // "ee8cad998726c7e3ac038f04e81743cb3c2f1e9ffc22587e1d8d48e52a35791f"
        iphone.setToken("dd01c9529008759b4fca35eb3c4c504cd1557f42c4370e90bf346820ce6018be");

        List<Device> dispositivos = new ArrayList<Device>();
        dispositivos.add(iphone);

        Notification notificacion = new Notification();
        notificacion.setTitulo("Pruebas con iPhone");
        notificacion.setMensaje("Probando mensajería con iPhone");
        ApplePush applePush = new ApplePush(dispositivoDao);
        long correctos = applePush.sendApple(notificacion, dispositivos, false);

        if (correctos == 1) {
            return "Funciona!!";
        } else {
            return "ERROR";
        }
    }
}