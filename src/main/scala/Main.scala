package scalacard

import scala.annotation.tailrec
import scala.io.StdIn
import scala.util.Try


object Main {

  def main( args: Array[String]): Unit = {
  
    println("Welcome to ScalaCard, the world's greatest Monopoly Deal hobby project!")
    println("")
    println(s"How many players? [${Rules.minPlayers} -> ${Rules.maxPlayers}]")
    val n = validateIntInput
    val players = n.fold(e => {
      println(e)
      return
    }, l => {
      println(s"There are $l players.")
      l
    })

    println("Get Ready!")
    var g = Gameplay.deal(Gameplay.newGame(players))

    while(g.deck.nonEmpty && !g.over){
      g = inputPrompt(g)
    }

  }

  type PIn = Either[String, Int]
  private def validateIntInput: PIn = {
   (1 to 3).foldLeft[PIn](Left[String, Int]("abc"))((l, r) => {
       l match{
         case l@Right(_) => l
         case _ => {
           Try{ StdIn.readInt() }.toOption
             .fold[PIn]{
             println(s"Please enter a NUMBER between ${Rules.minPlayers}-${Rules.maxPlayers}")
             Left(s"Please enter a NUMBER between ${Rules.minPlayers}-${Rules.maxPlayers}")
           } { i =>
             if(i <= Rules.maxPlayers && Rules.minPlayers <= i) Right(i)
             else {
               println(s"$i was not between ${Rules.minPlayers} & ${Rules.maxPlayers}. Lets try to play be the rules.")
               Left(s"Looks like you don't really want to play...")
             }
           }}}
   })
 }

 //Translate this over to a state monad
  def inputPrompt(g: Game): Game = {
    val prmpt = s"""
       |What would you like to do?
       |
       |Show Table  (t)
       |Show Hand   (h)
       |Play Card   (c)
       |End Turn    (e)
       |
       |Quit        (q)
     """.stripMargin
    println(prmpt)


    val in = StdIn.readChar() match {
      case 'e' => EndTurn
      case 't' => ShowTable
      case 'c' => PlayHandCard
      case 'h' => ShowHand
      case 'q' => Quit
    }
    CliInterpreter.interpret(in, g)
  }




}

trait MonopolyDrawCmd
case object ShowTable extends MonopolyDrawCmd
case object ShowHand extends MonopolyDrawCmd
case object PlayHandCard extends MonopolyDrawCmd
case object EndTurn extends MonopolyDrawCmd
case object Quit extends MonopolyDrawCmd
trait InputInterpreter {


  def interpret(cmd: MonopolyDrawCmd, g: Game): Game  = {
    cmd match{
      case ShowTable =>
        showTable(g)
        g
      case ShowHand => {
        showHand(g.player1.hand)
        g
      }
      case PlayHandCard => {
        val c = selectCard(g.player1.hand)
        Gameplay.playCard(c, g, choosePlayAsMoney(c))
      }
      case EndTurn => Gameplay.advanceTurn(g)
      case Quit => g.copy(over = true)
    }
  }



  def showTable(g: Game): Unit
  def showHand(h: Hand): Unit
  def selectCard(h: Hand): Card
  def choosePlayAsMoney(c: Card): Boolean

}




object CliInterpreter extends InputInterpreter {

  def showHand(h: Hand): Unit = {
    println("Your hand contains: ")
    val str = h.zipWithIndex.map(c => s"[ ${c._2} ] ${c._1} ") mkString "\n"
    println(str)
  }

  def showTable(g: Game) = {
    val playerTallies = g.players.zipWithIndex.map(p => toPlayerDisplay(p._1, p._2))
    val tbls = playerTallies.foldLeft[List[String]](Nil)((acc, pd) =>{
      val ls = acc.zipAll(pd, "", "")
      ls.map(ps => s" ${ps._1} | ${ps._2}")
    }).mkString("\n")
    println(tbls)
  }

  def selectCard(h: Hand): Card = {
    showHand(h)
    println("Which card will you play? (INT)")
    Try{ StdIn.readInt() }.toOption.fold{
      println("Invalid input.")
      selectCard(h)
    }{i => if (i >= 0 && i < h.length) h(i) else {
      println("Inalid input.")
      selectCard(h)
    }}
  }

  def choosePlayAsMoney(c: Card) = {
    println("Would you like to play this card as Money? (y/n)")
    val in = StdIn.readChar()
    in == 'y'
  }

  private def toPlayerDisplay(p: Player, n: Int): List[String] = {
    import MoneyConversion._
    val ls = s"Player-$n" ::
    "-----------------" ::
    s"Bank:  ${Player.moneyValue(p)}" ::
    " " ::
    p.props.map(prop => s" ${prop._2}  ${prop._1.toString}").toList

    val longestLine = ls.maxBy(_.length).length
    ls.map(s => s + List.fill(longestLine - s.length)(" ").mkString(""))
  }


}
