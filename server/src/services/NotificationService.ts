import admin from 'firebase-admin';
import { prisma } from '../prisma';

export interface PushNotification {
  title: string;
  body: string;
  type: 'attack' | 'takeover' | 'clan_invite' | 'info';
  data?: Record<string, string>;
}

/**
 * Сервис для отправки push-уведомлений через Firebase Cloud Messaging
 */
export class NotificationService {
  /**
   * Отправить уведомление конкретному пользователю
   */
  static async sendToUser(userId: string, notification: PushNotification) {
    try {
      const user = await prisma.user.findUnique({
        where: { id: userId },
        select: { fcmToken: true }
      });

      if (!user?.fcmToken) {
        console.log(`No FCM token for user ${userId}`);
        return;
      }

      const message = {
        notification: {
          title: notification.title,
          body: notification.body
        },
        data: {
          type: notification.type,
          ...(notification.data || {})
        },
        token: user.fcmToken
      };

      const response = await admin.messaging().send(message as any);
      console.log(`Notification sent to ${userId}:`, response);
      return response;
    } catch (error) {
      console.error(`Failed to send notification to ${userId}:`, error);
    }
  }

  /**
   * Отправить уведомление об атаке на территорию
   */
  static async notifyTerritoryAttack(
    victimId: string,
    attackerId: string,
    attackerName: string,
    areaM2: number
  ) {
    const areaStr = areaM2 >= 1_000_000
      ? `${(areaM2 / 1_000_000).toFixed(2)} км²`
      : `${Math.round(areaM2)} м²`;

    await this.sendToUser(victimId, {
      title: '⚠️ Атака на территорию!',
      body: `${attackerName} атакует вашу территорию (${areaStr})`,
      type: 'attack',
      data: {
        attackerId,
        area: areaM2.toString()
      }
    });
  }

  /**
   * Отправить уведомление об отъёме территории
   */
  static async notifyTerritoryTakeover(
    victimId: string,
    attackerId: string,
    attackerName: string,
    areaM2: number
  ) {
    const areaStr = areaM2 >= 1_000_000
      ? `${(areaM2 / 1_000_000).toFixed(2)} км²`
      : `${Math.round(areaM2)} м²`;

    await this.sendToUser(victimId, {
      title: '💥 Территория захвачена!',
      body: `${attackerName} полностью захватил вашу территорию (${areaStr})`,
      type: 'takeover',
      data: {
        attackerId,
        area: areaM2.toString()
      }
    });
  }

  /**
   * Отправить уведомление о частичном отъёме
   */
  static async notifyPartialTakeover(
    victimId: string,
    attackerId: string,
    attackerName: string,
    lostAreaM2: number
  ) {
    const areaStr = lostAreaM2 >= 1_000_000
      ? `${(lostAreaM2 / 1_000_000).toFixed(2)} км²`
      : `${Math.round(lostAreaM2)} м²`;

    await this.sendToUser(victimId, {
      title: '⚠️ Часть территории потеряна',
      body: `${attackerName} отобрал ${areaStr} вашей территории`,
      type: 'attack',
      data: {
        attackerId,
        area: lostAreaM2.toString()
      }
    });
  }

  /**
   * Уведомить лидера клана о новой заявке на вступление
   */
  static async notifyClanJoinRequest(
    leaderId: string,
    applicantName: string,
    clanId: string,
    clanName: string
  ) {
    await this.sendToUser(leaderId, {
      title: '📋 Новая заявка в клан',
      body: `${applicantName} хочет вступить в клан "${clanName}"`,
      type: 'clan_invite',
      data: { clanId, applicantName },
    });
  }

  /**
   * Отправить приглашение в клан
   */
  static async notifyClanInvite(userId: string, clanName: string, clanId: string) {
    await this.sendToUser(userId, {
      title: '🎖️ Приглашение в клан',
      body: `Вас пригласили в клан "${clanName}"`,
      type: 'clan_invite',
      data: {
        clanId
      }
    });
  }
}
