import { Component, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Icon } from '../../shared/icon';
import { Logo } from '../../shared/logo';
import { DemoForm } from './demo-form';

interface Feature {
  icon: string;
  title: string;
  text: string;
}

interface Plan {
  name: string;
  price: string;
  unit: string;
  pitch: string;
  features: string[];
  highlighted: boolean;
  cta: string;
}

interface Question {
  q: string;
  a: string;
}

/**
 * Page d accueil commerciale.
 *
 * C est la vitrine du produit : elle s adresse au dirigeant d une societe
 * de maintenance, pas a l utilisateur quotidien. Elle enonce donc le
 * probleme avant la solution, montre un resultat chiffre, et conduit a un
 * seul geste : demander une demonstration.
 *
 * Cette demande passe par un formulaire enregistre cote serveur, et non
 * par une adresse mailto : celle-ci n ouvre rien chez un visiteur sans
 * logiciel de messagerie configure, c est-a-dire chez la plupart. Un
 * bouton qui ne fait rien coute un prospect a chaque clic.
 */
@Component({
  selector: 'app-landing',
  imports: [RouterLink, Icon, Logo, DemoForm],
  templateUrl: './landing.html',
  styleUrl: './landing.scss',
})
export class LandingPage {
  protected readonly year = new Date().getFullYear();

  /** Formulaire de demande, affiche par-dessus la page. */
  protected readonly formOpen = signal(false);

  protected openForm(): void {
    this.formOpen.set(true);
  }

  protected closeForm(): void {
    this.formOpen.set(false);
  }

  /** Question dont la reponse est depliee, null si toutes sont fermees. */
  protected readonly openQuestion = signal<number | null>(0);

  protected readonly features: Feature[] = [
    {
      icon: 'ticket',
      title: 'Chaque appel devient un ticket',
      text:
        'Un numéro, une priorité, un responsable, une échéance. Plus rien ne ' +
        'dépend de la mémoire de celui qui a décroché le téléphone.',
    },
    {
      icon: 'clock',
      title: 'Des délais que vous tenez',
      text:
        'Le délai de résolution est calculé dès la déclaration. Le technicien ' +
        'est prévenu avant l échéance, et tout dépassement est constaté ' +
        'automatiquement.',
    },
    {
      icon: 'qr',
      title: 'Une étiquette QR par machine',
      text:
        'Le gérant scanne le code collé sur la caisse, décrit la panne, et ' +
        'c’est déclaré. Pas de compte à chercher, pas de liste à parcourir.',
    },
    {
      icon: 'chart',
      title: 'Des chiffres pour vos contrats',
      text:
        'Délai moyen de résolution, taux de respect des engagements, matériel ' +
        'le plus défaillant. De quoi renégocier en position de force.',
    },
    {
      icon: 'bell',
      title: 'Tout le monde au courant, en direct',
      text:
        'Affectation, changement de statut, nouveau message : chacun reçoit ' +
        'ce qui le concerne, immédiatement.',
    },
    {
      icon: 'shield',
      title: 'Chaque client chez lui',
      text:
        'Deux établissements concurrents peuvent utiliser la même plateforme ' +
        'sans jamais voir les données de l’autre.',
    },
  ];

  protected readonly steps = [
    {
      number: '01',
      title: 'Le gérant signale',
      text: 'Depuis son téléphone, en scannant l’étiquette de la machine ou en quelques champs.',
    },
    {
      number: '02',
      title: 'Vous affectez',
      text: 'La file d’attente affiche les urgences en tête. Un clic pour confier le ticket au bon technicien.',
    },
    {
      number: '03',
      title: 'Le technicien intervient',
      text: 'Il suit l’avancement, échange avec le client, et documente la solution appliquée.',
    },
    {
      number: '04',
      title: 'Le client confirme',
      text: 'Il valide la résolution et note la prestation. Le dossier est clos, et archivé.',
    },
  ];

  protected readonly plans: Plan[] = [
    {
      name: 'Essentiel',
      price: '89',
      unit: 'DT / mois',
      pitch: 'Pour une société qui demarre son activité de maintenance.',
      features: [
        "Jusqu’à 10 établissements clients",
        'Tickets et échanges illimités',
        'Délais contractuels et alertes',
        'Étiquettes QR illimitées',
        'Tableau de bord',
        'Assistance par courriel',
      ],
      highlighted: false,
      cta: 'Commencer',
    },
    {
      name: 'Professionnel',
      price: '219',
      unit: 'DT / mois',
      pitch: 'Pour une équipe structurée avec plusieurs techniciens.',
      features: [
        'Établissements clients illimités',
        'Tout le contenu de la formule Essentiel',
        'Charge de travail par technicien',
        'Historique complet et piste d’audit',
        'Export des données',
        'Assistance prioritaire',
      ],
      highlighted: true,
      cta: 'Demander une démonstration',
    },
    {
      name: 'Sur mesure',
      price: 'Nous consulter',
      unit: '',
      pitch: 'Pour un déploiement à votre nom et à vos règles.',
      features: [
        'Tout le contenu de la formule Professionnelle',
        'Votre logo et vos couleurs',
        'Délais contractuels adaptés par contrat',
        'Hébergement dédié',
        'Reprise de vos données existantes',
        'Formation de vos équipes',
      ],
      highlighted: false,
      cta: 'Nous contacter',
    },
  ];

  protected readonly questions: Question[] = [
    {
      q: 'Faut-il installer quelque chose chez mes clients ?',
      a:
        'Non. Depanio fonctionne dans un navigateur, sur ordinateur comme sur ' +
        'téléphone. La seule chose à installer physiquement est une étiquette ' +
        'QR autocollante sur les machines, que vous imprimez depuis la ' +
        'plateforme.',
    },
    {
      q: 'Mes clients doivent-ils apprendre à utiliser un logiciel ?',
      a:
        'Un gérant de café n’a que trois gestes à connaître : scanner, décrire, ' +
        'confirmer. L’interface a été conçue pour quelqu’un qui a trente ' +
        'secondes entre deux services, pas pour un informaticien.',
    },
    {
      q: 'Que se passe-t-il si un délai est dépassé ?',
      a:
        'Le système le constaté seul, marque le ticket, prévient les ' +
        'responsables et inscrit l événement dans l historique. Vous savez donc ' +
        'où vous en êtes avant que le client ne vous appelle pour s’en plaindre.',
    },
    {
      q: 'Mes données et celles de mes clients sont-elles séparées ?',
      a:
        'Chaque établissement ne voit que ses propres tickets, son propre ' +
        'matériel et sa propre fiche. Cette séparation est appliquée par le ' +
        'serveur à chaque requête, pas seulement masquée dans l’affichage.',
    },
    {
      q: 'Puis-je récupérer mes données si j’arrête ?',
      a:
        'Oui. Vos données vous appartiennent et restent exportables à tout ' +
        'moment, sans frais et sans délai.',
    },
  ];

  protected toggleQuestion(index: number): void {
    this.openQuestion.update((current) => (current === index ? null : index));
  }
}
